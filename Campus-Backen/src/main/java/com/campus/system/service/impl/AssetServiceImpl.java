package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.enums.AssetChangeSource;
import com.campus.system.common.enums.AssetStatus;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.enums.RepairType;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.common.util.AssetNumberGenerator;
import com.campus.system.converter.AssetConverter;
import com.campus.system.dto.AssetEditDTO;
import com.campus.system.dto.AssetQueryDTO;
import com.campus.system.dto.AssetStatusChangeDTO;
import com.campus.system.entity.Asset;
import com.campus.system.entity.AssetCategory;
import com.campus.system.entity.AssetStatusLog;
import com.campus.system.entity.Building;
import com.campus.system.entity.Campus;
import com.campus.system.entity.LocationSnapshot;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.RepairRecord;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.AssetCategoryMapper;
import com.campus.system.mapper.AssetMapper;
import com.campus.system.mapper.AssetStatusLogMapper;
import com.campus.system.mapper.BuildingMapper;
import com.campus.system.mapper.CampusMapper;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.mapper.RepairRecordMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.AssetService;
import com.campus.system.service.interfaces.LocationService;
import com.campus.system.service.interfaces.OssService;
import com.campus.system.vo.AssetRepairHistoryVO;
import com.campus.system.vo.AssetVO;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// 资产台账业务实现
@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private AssetCategoryMapper categoryMapper;

    @Autowired
    private AssetStatusLogMapper statusLogs;

    @Autowired
    private CampusMapper campusMapper;

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private RepairOrderMapper orderMapper;

    @Autowired
    private RepairRecordMapper recordMapper;

    @Autowired
    private RepairCategoryMapper repairCategoryMapper;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private LocationService locationService;

    @Autowired
    private OssService oss;

    @Autowired
    private AssetNumberGenerator numberGenerator;

    @Autowired
    private BusinessClock clock;

    @Override
    public PageResult<AssetVO> search(int pageNum, int pageSize, AssetQueryDTO query) {
        CurrentUser me = SecurityUtils.current();
        AssetQueryDTO safeQuery = query == null ? new AssetQueryDTO() : query;
        boolean includeDeleted = Boolean.TRUE.equals(safeQuery.getIncludeDeleted()) && "ADMIN".equals(me.roleCode());
        LambdaQueryWrapper<Asset> wrapper = buildQueryWrapper(me, safeQuery);
        IPage<Asset> page = includeDeleted ? selectPageIgnoringLogicDelete(pageNum, pageSize, wrapper) : assetMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return enrichPage(page);
    }

    @Override
    public AssetVO detail(Long id) {
        Asset asset = requireVisibleAsset(id);
        return enrichSingle(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO create(AssetEditDTO body) {
        requireAdmin();
        Asset asset = new Asset();
        asset.setAssetNo(numberGenerator.nextAssetNo(clock.today()));
        applyEditable(asset, body, true);
        asset.setStatus(AssetStatus.IN_USE.getCode());
        asset.setVersion(0);
        assetMapper.insert(asset);
        audit("CREATE_ASSET", asset.getAssetId(), "新增资产 " + asset.getAssetNo());
        return enrichSingle(assetMapper.selectById(asset.getAssetId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetVO update(Long id, AssetEditDTO body) {
        requireAdmin();
        Asset asset = requireActiveAsset(id);
        assertNoActiveOrder(asset);
        int version = required(body.getVersion(), "version");
        applyEditable(asset, body, false);
        if (assetMapper.update(asset, Wrappers.<Asset>lambdaUpdate().eq(Asset::getAssetId, id).eq(Asset::getDeleteState, 0)
                .isNull(Asset::getActiveOrderId).eq(Asset::getVersion, version).setSql("version = version + 1")) != 1) {
            throw BusinessException.conflict("资产已更新或存在未结束关联工单，请刷新后重试");
        }
        audit("UPDATE_ASSET", id, "修改资产 " + asset.getAssetNo());
        return enrichSingle(assetMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, AssetStatusChangeDTO body) {
        requireAdmin();
        Asset asset = requireActiveAsset(id);
        String target = required(body.getStatus(), "status");
        AssetStatus targetStatus = AssetStatus.of(target);
        if (targetStatus == AssetStatus.OUT_OF_SERVICE) {
            assertNoActiveOrder(asset);
        }
        int version = required(body.getVersion(), "version");
        if (assetMapper.updateStatusIfNoActiveOrder(id, targetStatus.getCode(), version) != 1) {
            throw BusinessException.conflict("资产状态不可变更，请确认是否存在未结束关联工单");
        }
        writeStatusLog(asset.getAssetId(), asset.getStatus(), targetStatus.getCode(), AssetChangeSource.ADMIN_MANUAL.getCode(),
                null, SecurityUtils.current().userId(), body.getChangeReason());
        audit("CHANGE_ASSET_STATUS", id, "资产 " + asset.getAssetNo() + " 状态变更为 " + targetStatus.getLabel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Integer version) {
        requireAdmin();
        Asset asset = requireActiveAsset(id);
        if (assetMapper.logicDeleteIfNoActiveOrder(id, required(version, "version")) != 1) {
            throw BusinessException.conflict("资产存在未结束关联工单，不能删除");
        }
        audit("DELETE_ASSET", id, "逻辑删除资产 " + asset.getAssetNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        requireAdmin();
        Asset asset = findByIdIgnoringLogicDelete(id);
        if (asset == null || asset.getDeleteState() != 1) {
            throw BusinessException.conflict("仅已删除资产可恢复");
        }
        Asset existingNo = assetMapper.selectOne(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetNo, asset.getAssetNo()).eq(Asset::getDeleteState, 0));
        if (existingNo != null) {
            throw BusinessException.conflict("资产编号已被占用，不能恢复");
        }
        requireEnabledCategory(asset.getAssetCategoryId());
        locationService.resolveSnapshot(asset.getCampusId(), asset.getBuildingId(), asset.getLocationDetail());
        if (assetMapper.restoreDeleted(id) != 1) {
            throw BusinessException.conflict("资产恢复失败");
        }
        audit("RESTORE_ASSET", id, "恢复资产 " + asset.getAssetNo());
    }

    @Override
    public PageResult<AssetRepairHistoryVO> repairHistory(Long assetId, int pageNum, int pageSize) {
        requireVisibleAsset(assetId);
        CurrentUser me = SecurityUtils.current();
        LambdaQueryWrapper<RepairOrder> wrapper = Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getAssetId, assetId).eq(RepairOrder::getDeleteState, 0)
                .eq(RepairOrder::getStatus, RepairStatus.COMPLETED.getCode()).orderByDesc(RepairOrder::getCompletionTime);
        if ("STUDENT".equals(me.roleCode()) || "TEACHER".equals(me.roleCode())) {
            wrapper.eq(RepairOrder::getReporterId, me.userId());
        }
        IPage<RepairOrder> page = orderMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<AssetRepairHistoryVO> items = page.getRecords().stream().map(this::toHistoryVO).toList();
        return new PageResult<>(items, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public String uploadImage(MultipartFile file) throws Exception {
        requireAdmin();
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024 || ImageIO.read(file.getInputStream()) == null) {
            throw BusinessException.badRequest("仅支持 20MB 内的有效图片");
        }
        String name = file.getOriginalFilename();
        String extension = name != null && name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "jpg";
        return oss.upload(extension, file.getInputStream());
    }

    @Override
    public void applyAssetDraft(RepairOrder order, String repairType, Long assetId) {
        RepairType type = repairType == null ? RepairType.NORMAL : RepairType.of(repairType);
        order.setRepairType(type.getCode());
        if (type == RepairType.NORMAL) {
            order.setAssetId(null);
            order.setAssetNoSnapshot(null);
            order.setAssetNameSnapshot(null);
            order.setAssetCategorySnapshot(null);
            order.setAssetLocationSnapshot(null);
            return;
        }
        Asset asset = requireAssetForRepair(assetId);
        fillSnapshots(order, asset);
        LocationSnapshot snapshot = locationService.resolveSnapshot(asset.getCampusId(), asset.getBuildingId(),
                asset.getLocationDetail() != null ? asset.getLocationDetail() : "资产位置");
        order.setCampusId(snapshot.getCampusId());
        order.setCampus(snapshot.getCampusName());
        order.setCampusDescriptionSnapshot(snapshot.getCampusDescription());
        order.setBuildingId(snapshot.getBuildingId());
        order.setBuilding(snapshot.getBuildingName());
        order.setBuildingDescriptionSnapshot(snapshot.getBuildingDescription());
        order.setFloor(asset.getFloor());
        order.setRoom(asset.getRoom());
        order.setLocationDetail(asset.getLocationDetail() != null ? asset.getLocationDetail() : snapshot.getCampusName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimAssetOnSubmit(RepairOrder order) {
        if (!RepairType.ASSET.getCode().equals(order.getRepairType()) || order.getAssetId() == null) {
            return;
        }
        Asset asset = assetMapper.selectById(order.getAssetId());
        if (asset == null || asset.getDeleteState() != 0 || !AssetStatus.IN_USE.getCode().equals(asset.getStatus())) {
            throw BusinessException.conflict("关联资产当前不可报修");
        }
        if (asset.getActiveOrderId() != null) {
            throw BusinessException.conflict("该资产已有未结束关联工单，不能重复提交");
        }
        if (assetMapper.claimActiveOrder(asset.getAssetId(), order.getOrderId(), asset.getVersion()) != 1) {
            throw BusinessException.conflict("该资产已被其他工单占用，请刷新后重试");
        }
        fillSnapshots(order, assetMapper.selectById(asset.getAssetId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onOrderAccepted(RepairOrder order) {
        if (!RepairType.ASSET.getCode().equals(order.getRepairType()) || order.getAssetId() == null) {
            return;
        }
        Asset asset = assetMapper.selectById(order.getAssetId());
        if (asset == null) {
            return;
        }
        if (assetMapper.markUnderRepair(asset.getAssetId(), order.getOrderId()) == 1) {
            writeStatusLog(asset.getAssetId(), AssetStatus.IN_USE.getCode(), AssetStatus.UNDER_REPAIR.getCode(), AssetChangeSource.ORDER_LINK.getCode(), order.getOrderId(), null, "关联工单接单或派单");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseOnWithdrawToDraft(RepairOrder order) {
        if (order == null || order.getAssetId() == null) {
            return;
        }
        Asset asset = assetMapper.selectById(order.getAssetId());
        if (asset == null) {
            return;
        }
        assetMapper.releaseActiveOrder(asset.getAssetId(), order.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repairStaleActiveOrderLink(Long assetId) {
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null || asset.getActiveOrderId() == null) {
            return;
        }
        RepairOrder order = orderMapper.selectById(asset.getActiveOrderId());
        boolean stale = order == null || order.getDeleteState() != 0 || order.getStatus() == RepairStatus.DRAFT.getCode()
                || List.of(RepairStatus.COMPLETED.getCode(), RepairStatus.REJECTED.getCode(), RepairStatus.CLOSED.getCode()).contains(order.getStatus());
        if (stale) {
            assetMapper.releaseActiveOrder(asset.getAssetId(), asset.getActiveOrderId());
            Asset refreshed = assetMapper.selectById(assetId);
            if (refreshed != null && AssetStatus.UNDER_REPAIR.getCode().equals(refreshed.getStatus())) {
                assetMapper.restoreInUseIfNoActiveOrder(assetId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onOrderEnded(RepairOrder order) {
        if (!RepairType.ASSET.getCode().equals(order.getRepairType()) || order.getAssetId() == null) {
            return;
        }
        Asset asset = assetMapper.selectById(order.getAssetId());
        if (asset == null) {
            return;
        }
        assetMapper.releaseActiveOrder(asset.getAssetId(), order.getOrderId());
        asset = assetMapper.selectById(order.getAssetId());
        if (asset == null || AssetStatus.OUT_OF_SERVICE.getCode().equals(asset.getStatus())) {
            return;
        }
        if (AssetStatus.UNDER_REPAIR.getCode().equals(asset.getStatus()) && asset.getActiveOrderId() == null) {
            assetMapper.restoreInUseIfNoActiveOrder(asset.getAssetId());
            writeStatusLog(asset.getAssetId(), AssetStatus.UNDER_REPAIR.getCode(), AssetStatus.IN_USE.getCode(),
                    AssetChangeSource.ORDER_END.getCode(), order.getOrderId(), null, "关联工单结束");
        }
    }

    @Override
    public Asset requireAssetForRepair(Long assetId) {
        Asset asset = assetMapper.selectById(required(assetId, "assetId"));
        if (asset == null || asset.getDeleteState() != 0) {
            throw BusinessException.badRequest("请选择有效资产");
        }
        if (!AssetStatus.IN_USE.getCode().equals(asset.getStatus())) {
            throw BusinessException.badRequest("仅使用中资产可提交关联报修");
        }
        if (asset.getActiveOrderId() != null) {
            throw BusinessException.conflict("该资产已有未结束关联工单");
        }
        requireEnabledCategory(asset.getAssetCategoryId());
        return asset;
    }

    private IPage<Asset> selectPageIgnoringLogicDelete(int pageNum, int pageSize, LambdaQueryWrapper<Asset> wrapper) {
        Page<Asset> page = Page.of(pageNum, pageSize);
        Long total = assetMapper.selectCountIncludeDeleted(wrapper);
        page.setTotal(total == null ? 0L : total);
        if (total == null || total == 0L) {
            page.setRecords(List.of());
            return page;
        }
        LambdaQueryWrapper<Asset> listWrapper = wrapper.clone();
        long offset = (long) (pageNum - 1) * pageSize;
        listWrapper.last("LIMIT " + offset + "," + pageSize);
        page.setRecords(assetMapper.selectListIncludeDeleted(listWrapper));
        return page;
    }

    private Asset findByIdIgnoringLogicDelete(Long id) {
        return assetMapper.selectByIdIncludeDeleted(id);
    }

    private LambdaQueryWrapper<Asset> buildQueryWrapper(CurrentUser me, AssetQueryDTO query) {
        LambdaQueryWrapper<Asset> wrapper = Wrappers.lambdaQuery();
        if (!Boolean.TRUE.equals(query.getIncludeDeleted()) || !"ADMIN".equals(me.roleCode())) {
            wrapper.eq(Asset::getDeleteState, 0);
        }
        if ("STUDENT".equals(me.roleCode()) || "TEACHER".equals(me.roleCode())) {
            wrapper.eq(Asset::getStatus, AssetStatus.IN_USE.getCode());
        }
        if (query.getAssetNo() != null && !query.getAssetNo().isBlank()) {
            wrapper.eq(Asset::getAssetNo, query.getAssetNo().trim());
        }
        if (query.getAssetNameKeyword() != null && !query.getAssetNameKeyword().isBlank()) {
            wrapper.like(Asset::getAssetName, query.getAssetNameKeyword().trim());
        }
        if (query.getAssetCategoryId() != null) {
            wrapper.eq(Asset::getAssetCategoryId, query.getAssetCategoryId());
        }
        if (query.getCampusId() != null) {
            wrapper.eq(Asset::getCampusId, query.getCampusId());
        }
        if (query.getBuildingId() != null) {
            wrapper.eq(Asset::getBuildingId, query.getBuildingId());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Asset::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Asset::getCreateTime);
        return wrapper;
    }

    private PageResult<AssetVO> enrichPage(IPage<Asset> page) {
        List<AssetVO> vos = enrichList(page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private List<AssetVO> enrichList(List<Asset> assets) {
        if (assets.isEmpty()) {
            return List.of();
        }
        Set<Long> categoryIds = assets.stream().map(Asset::getAssetCategoryId).collect(java.util.stream.Collectors.toSet());
        Set<Long> campusIds = assets.stream().map(Asset::getCampusId).collect(java.util.stream.Collectors.toSet());
        Set<Long> buildingIds = assets.stream().map(Asset::getBuildingId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, AssetCategory> categoryMap = categoryMapper.selectByIds(categoryIds).stream().collect(java.util.stream.Collectors.toMap(AssetCategory::getAssetCategoryId, c -> c));
        Map<Long, Campus> campusMap = campusMapper.selectByIds(campusIds).stream().collect(java.util.stream.Collectors.toMap(Campus::getCampusId, c -> c));
        Map<Long, Building> buildingMap = buildingIds.isEmpty() ? Map.of() : buildingMapper.selectByIds(buildingIds).stream().collect(java.util.stream.Collectors.toMap(Building::getBuildingId, b -> b));
        return assets.stream().map(asset -> enrichAsset(asset, categoryMap, campusMap, buildingMap)).toList();
    }

    private AssetVO enrichSingle(Asset asset) {
        Map<Long, AssetCategory> categoryMap = new HashMap<>();
        Map<Long, Campus> campusMap = new HashMap<>();
        Map<Long, Building> buildingMap = new HashMap<>();
        AssetCategory category = categoryMapper.selectById(asset.getAssetCategoryId());
        if (category != null) {
            categoryMap.put(category.getAssetCategoryId(), category);
        }
        Campus campus = campusMapper.selectById(asset.getCampusId());
        if (campus != null) {
            campusMap.put(campus.getCampusId(), campus);
        }
        if (asset.getBuildingId() != null) {
            Building building = buildingMapper.selectById(asset.getBuildingId());
            if (building != null) {
                buildingMap.put(building.getBuildingId(), building);
            }
        }
        return enrichAsset(asset, categoryMap, campusMap, buildingMap);
    }

    private AssetVO enrichAsset(Asset asset, Map<Long, AssetCategory> categoryMap, Map<Long, Campus> campusMap, Map<Long, Building> buildingMap) {
        repairStaleActiveOrderLink(asset.getAssetId());
        Asset refreshed = assetMapper.selectById(asset.getAssetId());
        if (refreshed == null) {
            refreshed = assetMapper.selectByIdIncludeDeleted(asset.getAssetId());
        }
        if (refreshed == null) {
            throw BusinessException.notFound("资产不存在");
        }
        asset = refreshed;
        AssetVO vo = AssetConverter.toAssetVO(asset);
        AssetCategory category = categoryMap.get(asset.getAssetCategoryId());
        if (category != null) {
            vo.setAssetCategoryName(category.getCategoryName());
        }
        Campus campus = campusMap.get(asset.getCampusId());
        if (campus != null) {
            vo.setCampusName(campus.getCampusName());
        }
        if (asset.getBuildingId() != null) {
            Building building = buildingMap.get(asset.getBuildingId());
            if (building != null) {
                vo.setBuildingName(building.getBuildingName());
            }
        }
        if (vo.getImageObjectKey() != null) {
            try {
                URL url = oss.signedUrl(vo.getImageObjectKey());
                if (url != null) {
                    vo.setImageSignedUrl(url.toString());
                }
            } catch (Exception ignored) {

            }
        }
        enrichPurchaseAge(vo, asset.getPurchaseDate());
        return vo;
    }

    private void enrichPurchaseAge(AssetVO vo, LocalDate purchaseDate) {
        if (purchaseDate == null) {
            return;
        }
        long months = ChronoUnit.MONTHS.between(purchaseDate, clock.today());
        if (months < 0) {
            months = 0;
        }
        vo.setPurchaseYears((int) (months / 12));
        vo.setPurchaseMonths((int) (months % 12));
    }

    private void applyEditable(Asset asset, AssetEditDTO body, boolean creating) {
        asset.setAssetName(required(body.getAssetName(), "assetName"));
        Long categoryId = required(body.getAssetCategoryId(), "assetCategoryId");
        requireEnabledCategory(categoryId);
        asset.setAssetCategoryId(categoryId);
        LocationSnapshot snapshot = locationService.resolveSnapshot(required(body.getCampusId(), "campusId"), body.getBuildingId(), body.getLocationDetail());
        asset.setCampusId(snapshot.getCampusId());
        asset.setBuildingId(snapshot.getBuildingId());
        asset.setFloor(trim(body.getFloor()));
        asset.setRoom(trim(body.getRoom()));
        asset.setLocationDetail(trim(body.getLocationDetail()));
        if (snapshot.getBuildingId() == null && (asset.getLocationDetail() == null || asset.getLocationDetail().isBlank())) {
            throw BusinessException.badRequest("未选择楼栋时必须填写具体位置描述");
        }
        if (body.getDescription() != null) {
            asset.setDescription(trim(body.getDescription()));
        }
        if (body.getEnabledDate() != null || creating) {
            asset.setEnabledDate(body.getEnabledDate());
        }
        if (body.getPurchaseDate() != null || creating) {
            asset.setPurchaseDate(body.getPurchaseDate());
        }
        if (body.getImageObjectKey() != null) {
            asset.setImageObjectKey(trim(body.getImageObjectKey()));
        }
    }

    private void fillSnapshots(RepairOrder order, Asset asset) {
        order.setAssetId(asset.getAssetId());
        order.setAssetNoSnapshot(asset.getAssetNo());
        order.setAssetNameSnapshot(asset.getAssetName());
        AssetCategory category = categoryMapper.selectById(asset.getAssetCategoryId());
        order.setAssetCategorySnapshot(category != null ? category.getCategoryName() : null);
        order.setAssetLocationSnapshot(buildLocationText(asset));
    }

    private String buildLocationText(Asset asset) {
        StringBuilder sb = new StringBuilder();
        Campus campus = campusMapper.selectById(asset.getCampusId());
        if (campus != null) {
            sb.append(campus.getCampusName());
        }
        if (asset.getBuildingId() != null) {
            Building building = buildingMapper.selectById(asset.getBuildingId());
            if (building != null) {
                sb.append(" ").append(building.getBuildingName());
            }
        }
        if (asset.getFloor() != null && !asset.getFloor().isBlank()) {
            sb.append(" ").append(asset.getFloor());
        }
        if (asset.getRoom() != null && !asset.getRoom().isBlank()) {
            sb.append(" ").append(asset.getRoom());
        }
        if (asset.getLocationDetail() != null && !asset.getLocationDetail().isBlank()) {
            sb.append(" ").append(asset.getLocationDetail());
        }
        return sb.toString().trim();
    }

    private AssetRepairHistoryVO toHistoryVO(RepairOrder order) {
        AssetRepairHistoryVO vo = new AssetRepairHistoryVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        RepairCategory category = repairCategoryMapper.selectById(order.getCategoryId());
        vo.setCategoryName(category != null ? category.getCategoryName() : null);
        vo.setDescription(order.getDescription());
        vo.setCompletionTime(order.getCompletionTime());
        if (order.getCurrentRepairerId() != null) {
            SysUser repairer = users.selectById(order.getCurrentRepairerId());
            if (repairer != null) {
                vo.setRepairerUserNo(repairer.getUserNo());
                vo.setRepairerRealName(repairer.getRealName());
            }
        }
        RepairRecord record = recordMapper.selectOne(Wrappers.<RepairRecord>lambdaQuery()
                .eq(RepairRecord::getOrderId, order.getOrderId()).orderByDesc(RepairRecord::getAttemptNo).last("LIMIT 1"));
        if (record != null) {
            vo.setRepairResult(record.getResultDescription());
        }
        return vo;
    }

    private void writeStatusLog(Long assetId, String before, String after, String source, Long orderId, Long operatorId, String reason) {
        AssetStatusLog log = new AssetStatusLog();
        log.setAssetId(assetId);
        log.setBeforeStatus(before);
        log.setAfterStatus(after);
        log.setChangeSource(source);
        log.setRelatedOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setChangeReason(reason);
        statusLogs.insert(log);
    }

    private Asset requireVisibleAsset(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw BusinessException.notFound("资产不存在");
        }
        CurrentUser me = SecurityUtils.current();
        if ("ADMIN".equals(me.roleCode()) || "REPAIRER".equals(me.roleCode())) {
            if (asset.getDeleteState() != 0) {
                throw BusinessException.notFound("资产不存在");
            }
            return asset;
        }
        if (("STUDENT".equals(me.roleCode()) || "TEACHER".equals(me.roleCode()))
                && asset.getDeleteState() == 0 && AssetStatus.IN_USE.getCode().equals(asset.getStatus())) {
            return asset;
        }
        throw BusinessException.forbidden("无权查看该资产");
    }

    private Asset requireActiveAsset(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null || asset.getDeleteState() != 0) {
            throw BusinessException.notFound("资产不存在");
        }
        return asset;
    }

    private void requireEnabledCategory(Long categoryId) {
        AssetCategory category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDeleteState() != 0 || category.getStatus() != 0) {
            throw BusinessException.badRequest("请选择启用的资产分类");
        }
    }

    private void assertNoActiveOrder(Asset asset) {
        if (asset.getActiveOrderId() != null) {
            throw BusinessException.conflict("资产存在未结束关联工单，不能执行此操作");
        }
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可操作");
        }
    }

    private void audit(String type, Long targetId, String description) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(SecurityUtils.current().userId());
        log.setOperationType(type);
        log.setTargetType("ASSET");
        log.setTargetId(targetId);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Long required(Long value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Integer required(Integer value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }
}
