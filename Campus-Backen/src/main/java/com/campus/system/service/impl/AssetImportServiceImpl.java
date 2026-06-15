package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.enums.AssetImportItemStatus;
import com.campus.system.common.enums.AssetImportSourceType;
import com.campus.system.common.enums.AiRecognizeStatus;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.converter.AssetImportConverter;
import com.campus.system.dto.AssetEditDTO;
import com.campus.system.dto.AssetImportBatchQueryDTO;
import com.campus.system.dto.AssetImportConfirmDTO;
import com.campus.system.dto.AssetImportItemUpdateDTO;
import com.campus.system.entity.Asset;
import com.campus.system.entity.AssetCategory;
import com.campus.system.entity.AssetImportBatch;
import com.campus.system.entity.AssetImportItem;
import com.campus.system.entity.Building;
import com.campus.system.entity.Campus;
import com.campus.system.entity.LocationSnapshot;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.AssetCategoryMapper;
import com.campus.system.mapper.AssetImportBatchMapper;
import com.campus.system.mapper.AssetImportItemMapper;
import com.campus.system.mapper.AssetMapper;
import com.campus.system.mapper.BuildingMapper;
import com.campus.system.mapper.CampusMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.dto.AiRecognizedAssetFields;
import com.campus.system.dto.RecognizedAssetRow;
import com.campus.system.service.asset.AssetAiRecognitionService;
import com.campus.system.service.asset.AssetCategorySemanticMatchService;
import com.campus.system.service.asset.AssetExcelRecognitionService;
import com.campus.system.service.interfaces.OssService;
import com.campus.system.service.interfaces.AssetImportService;
import com.campus.system.service.interfaces.AssetService;
import com.campus.system.service.interfaces.LocationService;
import com.campus.system.vo.AssetImportBatchVO;
import com.campus.system.vo.AssetImportConfirmFailureVO;
import com.campus.system.vo.AssetImportConfirmResultVO;
import com.campus.system.vo.AssetImportItemVO;
import com.campus.system.vo.AssetVO;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// 资产批量导入业务实现
@Service
public class AssetImportServiceImpl implements AssetImportService {

    @Autowired
    private AssetImportBatchMapper batchMapper;

    @Autowired
    private AssetImportItemMapper itemMapper;

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private AssetCategoryMapper categoryMapper;

    @Autowired
    private CampusMapper campusMapper;

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private AssetExcelRecognitionService recognitionService;

    @Autowired
    private AssetAiRecognitionService aiRecognitionService;

    @Autowired
    private AssetCategorySemanticMatchService categorySemanticMatch;

    @Autowired
    private OssService ossService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private BusinessClock clock;

    @Autowired
    @Lazy
    private AssetImportServiceImpl self;

    private static final int MAX_IMAGE_COUNT = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetImportBatchVO upload(MultipartFile file) {
        requireAdmin();
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请上传 Excel 文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw BusinessException.badRequest("仅支持 .xlsx 格式文件");
        }
        List<RecognizedAssetRow> rows;
        try {
            rows = recognitionService.recognize(file.getInputStream());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("无法读取 Excel 文件");
        }
        AssetImportBatch batch = new AssetImportBatch();
        batch.setFileName(fileName);
        batch.setSourceType(AssetImportSourceType.EXCEL.getCode());
        batch.setOperatorId(SecurityUtils.current().userId());
        batch.setTotalCount(rows.size());
        batch.setPendingCount(rows.size());
        batch.setConfirmedCount(0);
        batch.setIgnoredCount(0);
        batchMapper.insert(batch);
        for (RecognizedAssetRow row : rows) {
            applyAiEnhancement(row);
            AssetImportItem item = new AssetImportItem();
            item.setBatchId(batch.getBatchId());
            item.setRowNumber(row.getRowNumber());
            item.setAssetName(trim(row.getAssetName()));
            item.setCategoryText(trim(row.getCategoryText()));
            item.setAssetCategoryId(categorySemanticMatch.matchCategoryId(row.getCategoryText(), SecurityUtils.current().userId()));
            item.setPurchaseDate(row.getPurchaseDate());
            item.setEnabledDate(row.getEnabledDate());
            item.setAssetDescription(trim(row.getAssetDescription()));
            item.setLocationText(trim(row.getLocationText()));
            item.setStatus(AssetImportItemStatus.PENDING.getCode());
            item.setDuplicateHint(buildDuplicateHint(item.getAssetName(), item.getCategoryText(), item.getAssetCategoryId()));
            itemMapper.insert(item);
        }
        audit("UPLOAD_ASSET_IMPORT", batch.getBatchId(), "上传资产导入文件 " + fileName + "，识别 " + rows.size() + " 行");
        return enrichBatch(batchMapper.selectById(batch.getBatchId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetImportBatchVO uploadImages(MultipartFile[] files) {
        requireAdmin();
        if (files == null || files.length == 0) {
            throw BusinessException.badRequest("请上传图片");
        }
        if (files.length > MAX_IMAGE_COUNT) {
            throw BusinessException.badRequest("单次最多上传 10 张图片");
        }
        AssetImportBatch batch = new AssetImportBatch();
        batch.setFileName("图片导入_" + System.currentTimeMillis());
        batch.setSourceType(AssetImportSourceType.IMAGE.getCode());
        batch.setOperatorId(SecurityUtils.current().userId());
        batch.setTotalCount(files.length);
        batch.setPendingCount(files.length);
        batch.setConfirmedCount(0);
        batch.setIgnoredCount(0);
        batchMapper.insert(batch);
        int rowNo = 1;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw BusinessException.badRequest("存在空图片文件");
            }
            String objectKey;
            try {
                objectKey = ossService.upload(imageExtension(file), file.getInputStream());
            } catch (Exception ex) {
                throw BusinessException.badRequest("图片上传失败");
            }
            AssetImportItem item = new AssetImportItem();
            item.setBatchId(batch.getBatchId());
            item.setRowNumber(rowNo++);
            item.setSourceImageObjectKey(objectKey);
            item.setAiRecognizeStatus(AiRecognizeStatus.PENDING.getCode());
            item.setStatus(AssetImportItemStatus.PENDING.getCode());
            itemMapper.insert(item);
            self.recognizeImageAsync(item.getItemId(), objectKey);
        }
        audit("UPLOAD_ASSET_IMPORT_IMAGE", batch.getBatchId(), "上传图片导入 " + files.length + " 张");
        return enrichBatch(batchMapper.selectById(batch.getBatchId()));
    }

    @Async
    public void recognizeImageAsync(Long itemId, String objectKey) {
        AssetImportItem item = itemMapper.selectById(itemId);
        if (item == null) {
            return;
        }
        try {
            URL signedUrl = ossService.signedUrl(objectKey);
            AiRecognizedAssetFields fields = aiRecognitionService.recognizeImage(signedUrl.toString(), itemId);
            applyRecognizedFields(item, fields);
            item.setAiRecognizeStatus(fields == null ? AiRecognizeStatus.FAILED.getCode() : AiRecognizeStatus.SUCCESS.getCode());
        } catch (Exception ex) {
            item.setAiRecognizeStatus(AiRecognizeStatus.FAILED.getCode());
        }
        itemMapper.updateById(item);
    }

    @Override
    public PageResult<AssetImportBatchVO> listBatches(int pageNum, int pageSize, AssetImportBatchQueryDTO query) {
        requireAdmin();
        AssetImportBatchQueryDTO safeQuery = query == null ? new AssetImportBatchQueryDTO() : query;
        IPage<AssetImportBatch> page = batchMapper.selectPage(Page.of(pageNum, pageSize), buildBatchQueryWrapper(safeQuery));
        List<AssetImportBatchVO> records = enrichBatches(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private LambdaQueryWrapper<AssetImportBatch> buildBatchQueryWrapper(AssetImportBatchQueryDTO query) {
        LambdaQueryWrapper<AssetImportBatch> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AssetImportBatch::getDeleteState, 0);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(AssetImportBatch::getFileName, query.getKeyword().trim());
        }
        if (query.getSourceType() != null && !query.getSourceType().isBlank()) {
            wrapper.eq(AssetImportBatch::getSourceType, query.getSourceType().trim());
        }
        if (query.getCreateTimeFrom() != null) {
            wrapper.ge(AssetImportBatch::getCreateTime, query.getCreateTimeFrom());
        }
        if (query.getCreateTimeTo() != null) {
            wrapper.le(AssetImportBatch::getCreateTime, query.getCreateTimeTo());
        }
        if (Boolean.TRUE.equals(query.getOnlyPending())) {
            wrapper.gt(AssetImportBatch::getPendingCount, 0);
        }
        wrapper.orderByDesc(AssetImportBatch::getCreateTime);
        return wrapper;
    }

    @Override
    public AssetImportBatchVO getBatch(Long batchId) {
        requireAdmin();
        return enrichBatch(requireBatch(batchId));
    }

    @Override
    public PageResult<AssetImportItemVO> listBatchItems(Long batchId, String status, int pageNum, int pageSize) {
        requireAdmin();
        requireBatch(batchId);
        LambdaQueryWrapper<AssetImportItem> wrapper = Wrappers.<AssetImportItem>lambdaQuery()
                .eq(AssetImportItem::getBatchId, batchId).eq(AssetImportItem::getDeleteState, 0);
        if (status != null && !status.isBlank()) {
            wrapper.eq(AssetImportItem::getStatus, status.trim());
        }
        wrapper.orderByAsc(AssetImportItem::getRowNumber);
        IPage<AssetImportItem> page = itemMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return new PageResult<>(enrichItems(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetImportItemVO updateItem(Long itemId, AssetImportItemUpdateDTO body) {
        requireAdmin();
        AssetImportItem item = requirePendingItem(itemId);
        if (body.getAssetName() != null) {
            item.setAssetName(trim(body.getAssetName()));
        }
        if (body.getAssetCategoryId() != null) {
            requireEnabledCategory(body.getAssetCategoryId());
            item.setAssetCategoryId(body.getAssetCategoryId());
        }
        if (body.getPurchaseDate() != null) {
            item.setPurchaseDate(body.getPurchaseDate());
        }
        if (body.getEnabledDate() != null) {
            item.setEnabledDate(body.getEnabledDate());
        }
        if (body.getAssetDescription() != null) {
            item.setAssetDescription(trim(body.getAssetDescription()));
        }
        if (body.getLocationText() != null) {
            item.setLocationText(trim(body.getLocationText()));
        }
        if (body.getCampusId() != null) {
            LocationSnapshot snapshot = locationService.resolveSnapshot(body.getCampusId(), body.getBuildingId(), body.getLocationDetail());
            item.setCampusId(snapshot.getCampusId());
            item.setBuildingId(snapshot.getBuildingId());
            item.setLocationDetail(trim(body.getLocationDetail()));
        } else if (body.getBuildingId() != null || body.getLocationDetail() != null) {
            if (item.getCampusId() == null) {
                throw BusinessException.badRequest("请先选择校区");
            }
            LocationSnapshot snapshot = locationService.resolveSnapshot(item.getCampusId(), body.getBuildingId(), body.getLocationDetail());
            item.setBuildingId(snapshot.getBuildingId());
            if (body.getLocationDetail() != null) {
                item.setLocationDetail(trim(body.getLocationDetail()));
            }
        }
        if (body.getFloor() != null) {
            item.setFloor(trim(body.getFloor()));
        }
        if (body.getRoom() != null) {
            item.setRoom(trim(body.getRoom()));
        }
        item.setDuplicateHint(buildDuplicateHint(item.getAssetName(), item.getCategoryText(), item.getAssetCategoryId()));
        itemMapper.updateById(item);
        audit("UPDATE_ASSET_IMPORT_ITEM", itemId, "编辑待审核资产卡片 行号 " + item.getRowNumber());
        return enrichItem(itemMapper.selectById(itemId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignoreItem(Long itemId) {
        requireAdmin();
        AssetImportItem item = requirePendingItem(itemId);
        if (itemMapper.ignoreIfPending(itemId) != 1) {
            throw BusinessException.conflict("卡片状态已变化，请刷新后重试");
        }
        adjustBatchCounts(item.getBatchId(), AssetImportItemStatus.PENDING.getCode(), AssetImportItemStatus.IGNORED.getCode());
        audit("IGNORE_ASSET_IMPORT_ITEM", itemId, "忽略待审核资产卡片 行号 " + item.getRowNumber());
    }

    @Override
    public AssetImportConfirmResultVO confirmItem(Long itemId) {
        requireAdmin();
        AssetImportConfirmResultVO result = new AssetImportConfirmResultVO();
        result.setSuccessItemIds(new ArrayList<>());
        result.setFailures(new ArrayList<>());
        try {
            self.confirmItemInNewTx(itemId);
            result.setSuccessCount(1);
            result.getSuccessItemIds().add(itemId);
        } catch (BusinessException ex) {
            result.setFailureCount(1);
            AssetImportConfirmFailureVO failure = new AssetImportConfirmFailureVO();
            failure.setItemId(itemId);
            failure.setReason(ex.getMessage());
            result.getFailures().add(failure);
            itemMapper.markFailure(itemId, ex.getMessage());
        } catch (Exception ex) {
            result.setFailureCount(1);
            AssetImportConfirmFailureVO failure = new AssetImportConfirmFailureVO();
            failure.setItemId(itemId);
            failure.setReason("确认入库失败");
            result.getFailures().add(failure);
            itemMapper.markFailure(itemId, "确认入库失败");
        }
        return result;
    }

    @Override
    public AssetImportConfirmResultVO confirmBatch(AssetImportConfirmDTO body) {
        requireAdmin();
        List<Long> itemIds = resolveConfirmItemIds(body);
        if (itemIds.isEmpty()) {
            throw BusinessException.badRequest("当前批次没有待审核卡片");
        }
        AssetImportConfirmResultVO result = new AssetImportConfirmResultVO();
        result.setSuccessItemIds(new ArrayList<>());
        result.setFailures(new ArrayList<>());
        for (Long itemId : itemIds) {
            try {
                self.confirmItemInNewTx(itemId);
                result.setSuccessCount(result.getSuccessCount() + 1);
                result.getSuccessItemIds().add(itemId);
            } catch (BusinessException ex) {
                result.setFailureCount(result.getFailureCount() + 1);
                AssetImportConfirmFailureVO failure = new AssetImportConfirmFailureVO();
                failure.setItemId(itemId);
                failure.setReason(ex.getMessage());
                result.getFailures().add(failure);
                itemMapper.markFailure(itemId, ex.getMessage());
            } catch (Exception ex) {
                result.setFailureCount(result.getFailureCount() + 1);
                AssetImportConfirmFailureVO failure = new AssetImportConfirmFailureVO();
                failure.setItemId(itemId);
                failure.setReason("确认入库失败");
                result.getFailures().add(failure);
                itemMapper.markFailure(itemId, "确认入库失败");
            }
        }
        audit("CONFIRM_ASSET_IMPORT_BATCH", null, "批量确认资产卡片，成功 " + result.getSuccessCount() + " 条，失败 " + result.getFailureCount() + " 条");
        return result;
    }

    private List<Long> resolveConfirmItemIds(AssetImportConfirmDTO body) {
        if (body == null) {
            return List.of();
        }
        if (body.getItemIds() != null && !body.getItemIds().isEmpty()) {
            return body.getItemIds();
        }
        if (body.getBatchId() == null) {
            return List.of();
        }
        requireBatch(body.getBatchId());
        return itemMapper.selectList(Wrappers.<AssetImportItem>lambdaQuery().select(AssetImportItem::getItemId)
                        .eq(AssetImportItem::getBatchId, body.getBatchId()).eq(AssetImportItem::getStatus, AssetImportItemStatus.PENDING.getCode())
                        .eq(AssetImportItem::getDeleteState, 0).orderByAsc(AssetImportItem::getRowNumber)).stream().map(AssetImportItem::getItemId).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void confirmItemInNewTx(Long itemId) {
        AssetImportItem item = requirePendingItem(itemId);
        validateConfirmable(item);
        AssetEditDTO dto = toAssetEditDTO(item);
        AssetVO created = assetService.create(dto);
        if (itemMapper.confirmIfPending(itemId, AssetImportItemStatus.CONFIRMED.getCode(), created.getAssetId()) != 1) {
            throw BusinessException.conflict("卡片状态已变化，请刷新后重试");
        }
        adjustBatchCounts(item.getBatchId(), AssetImportItemStatus.PENDING.getCode(), AssetImportItemStatus.CONFIRMED.getCode());
        audit("CONFIRM_ASSET_IMPORT_ITEM", itemId, "确认入库资产卡片 行号 " + item.getRowNumber() + "，资产编号 " + created.getAssetNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long batchId) {
        requireAdmin();
        AssetImportBatch batch = requireBatch(batchId);
        if (batchMapper.logicDeleteIfPending(batchId) != 1) {
            throw BusinessException.conflict("仅含待审核卡片的批次可删除");
        }
        itemMapper.logicDeletePendingByBatch(batchId);
        audit("DELETE_ASSET_IMPORT_BATCH", batchId, "删除资产导入批次 " + batch.getFileName());
    }

    private AssetEditDTO toAssetEditDTO(AssetImportItem item) {
        AssetEditDTO dto = new AssetEditDTO();
        dto.setAssetName(item.getAssetName());
        dto.setAssetCategoryId(item.getAssetCategoryId());
        dto.setCampusId(item.getCampusId());
        dto.setBuildingId(item.getBuildingId());
        dto.setFloor(item.getFloor());
        dto.setRoom(item.getRoom());
        dto.setLocationDetail(item.getLocationDetail());
        dto.setPurchaseDate(item.getPurchaseDate());
        dto.setEnabledDate(item.getEnabledDate() != null ? item.getEnabledDate() : (item.getPurchaseDate() != null ? item.getPurchaseDate() : clock.today()));
        dto.setDescription(item.getAssetDescription());
        if (item.getSourceImageObjectKey() != null && !item.getSourceImageObjectKey().isBlank()) {
            dto.setImageObjectKey(item.getSourceImageObjectKey());
        }
        return dto;
    }

    private void validateConfirmable(AssetImportItem item) {
        if (item.getAssetName() == null || item.getAssetName().isBlank()) {
            throw BusinessException.badRequest("资产名称不能为空");
        }
        if (item.getAssetCategoryId() == null) {
            throw BusinessException.badRequest("请选择资产分类");
        }
        requireEnabledCategory(item.getAssetCategoryId());
        if (item.getCampusId() == null) {
            throw BusinessException.badRequest("请选择校区");
        }
        locationService.resolveSnapshot(item.getCampusId(), item.getBuildingId(), item.getLocationDetail());
        if (item.getBuildingId() == null && (item.getLocationDetail() == null || item.getLocationDetail().isBlank())) {
            throw BusinessException.badRequest("未选择楼栋时必须填写具体位置描述");
        }
    }

    private String buildDuplicateHint(String assetName, String categoryText, Long categoryId) {
        if (assetName == null || assetName.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<Asset> wrapper = Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getDeleteState, 0).eq(Asset::getAssetName, assetName.trim());
        if (categoryId != null) {
            wrapper.eq(Asset::getAssetCategoryId, categoryId);
        } else if (categoryText != null && !categoryText.isBlank()) {
            List<AssetCategory> categories = categoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery().eq(AssetCategory::getDeleteState, 0).eq(AssetCategory::getCategoryName, categoryText.trim()));
            if (categories.isEmpty()) {
                return null;
            }
            wrapper.in(Asset::getAssetCategoryId, categories.stream().map(AssetCategory::getAssetCategoryId).toList());
        } else {
            return null;
        }
        long count = assetMapper.selectCount(wrapper);
        if (count < 1) {
            return null;
        }
        return "系统中已存在 " + count + " 条同名同分类资产，请确认是否重复入库";
    }

    private void adjustBatchCounts(Long batchId, String fromStatus, String toStatus) {
        AssetImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        if (AssetImportItemStatus.PENDING.getCode().equals(fromStatus)) {
            batch.setPendingCount(Math.max(0, batch.getPendingCount() - 1));
        }
        if (AssetImportItemStatus.PENDING.getCode().equals(toStatus)) {
            batch.setPendingCount(batch.getPendingCount() + 1);
        }
        if (AssetImportItemStatus.CONFIRMED.getCode().equals(toStatus)) {
            batch.setConfirmedCount(batch.getConfirmedCount() + 1);
        }
        if (AssetImportItemStatus.IGNORED.getCode().equals(toStatus)) {
            batch.setIgnoredCount(batch.getIgnoredCount() + 1);
        }
        batchMapper.updateById(batch);
    }

    private List<AssetImportBatchVO> enrichBatches(List<AssetImportBatch> batches) {
        if (batches.isEmpty()) {
            return List.of();
        }
        Set<Long> operatorIds = batches.stream().map(AssetImportBatch::getOperatorId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = users.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getUserId, operatorIds))
                .stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));
        return batches.stream().map(batch -> {
            AssetImportBatchVO vo = AssetImportConverter.toBatchVO(batch);
            SysUser operator = userMap.get(batch.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getRealName());
            }
            return vo;
        }).toList();
    }

    private AssetImportBatchVO enrichBatch(AssetImportBatch batch) {
        return enrichBatches(List.of(batch)).get(0);
    }

    private List<AssetImportItemVO> enrichItems(List<AssetImportItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        Set<Long> categoryIds = items.stream().map(AssetImportItem::getAssetCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> campusIds = items.stream().map(AssetImportItem::getCampusId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> buildingIds = items.stream().map(AssetImportItem::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AssetCategory> categoryMap = categoryIds.isEmpty() ? Map.of() : categoryMapper.selectByIds(categoryIds).stream().collect(Collectors.toMap(AssetCategory::getAssetCategoryId, c -> c));
        Map<Long, Campus> campusMap = campusIds.isEmpty() ? Map.of() : campusMapper.selectByIds(campusIds).stream().collect(Collectors.toMap(Campus::getCampusId, c -> c));
        Map<Long, Building> buildingMap = buildingIds.isEmpty() ? Map.of() : buildingMapper.selectByIds(buildingIds).stream().collect(Collectors.toMap(Building::getBuildingId, b -> b));
        return items.stream().map(item -> enrichItem(item, categoryMap, campusMap, buildingMap)).toList();
    }

    private AssetImportItemVO enrichItem(AssetImportItem item) {
        Map<Long, AssetCategory> categoryMap = new HashMap<>();
        Map<Long, Campus> campusMap = new HashMap<>();
        Map<Long, Building> buildingMap = new HashMap<>();
        if (item.getAssetCategoryId() != null) {
            AssetCategory category = categoryMapper.selectById(item.getAssetCategoryId());
            if (category != null) {
                categoryMap.put(category.getAssetCategoryId(), category);
            }
        }
        if (item.getCampusId() != null) {
            Campus campus = campusMapper.selectById(item.getCampusId());
            if (campus != null) {
                campusMap.put(campus.getCampusId(), campus);
            }
        }
        if (item.getBuildingId() != null) {
            Building building = buildingMapper.selectById(item.getBuildingId());
            if (building != null) {
                buildingMap.put(building.getBuildingId(), building);
            }
        }
        return enrichItem(item, categoryMap, campusMap, buildingMap);
    }

    private AssetImportItemVO enrichItem(AssetImportItem item, Map<Long, AssetCategory> categoryMap,
            Map<Long, Campus> campusMap, Map<Long, Building> buildingMap) {
        AssetImportItemVO vo = AssetImportConverter.toItemVO(item);
        if (item.getAssetCategoryId() != null) {
            AssetCategory category = categoryMap.get(item.getAssetCategoryId());
            if (category != null) {
                vo.setAssetCategoryName(category.getCategoryName());
            }
        }
        if (item.getCampusId() != null) {
            Campus campus = campusMap.get(item.getCampusId());
            if (campus != null) {
                vo.setCampusName(campus.getCampusName());
            }
        }
        if (item.getBuildingId() != null) {
            Building building = buildingMap.get(item.getBuildingId());
            if (building != null) {
                vo.setBuildingName(building.getBuildingName());
            }
        }
        return vo;
    }

    private AssetImportBatch requireBatch(Long batchId) {
        AssetImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getDeleteState() != 0) {
            throw BusinessException.notFound("导入批次不存在");
        }
        return batch;
    }

    private AssetImportItem requirePendingItem(Long itemId) {
        AssetImportItem item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleteState() != 0) {
            throw BusinessException.notFound("资产卡片不存在");
        }
        if (!AssetImportItemStatus.PENDING.getCode().equals(item.getStatus())) {
            throw BusinessException.conflict("仅待审核卡片可执行此操作");
        }
        return item;
    }

    private void requireEnabledCategory(Long categoryId) {
        AssetCategory category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDeleteState() != 0 || category.getStatus() != 0) {
            throw BusinessException.badRequest("请选择启用的资产分类");
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
        log.setTargetType("ASSET_IMPORT");
        log.setTargetId(targetId);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void applyAiEnhancement(RecognizedAssetRow row) {
        StringBuilder rowText = new StringBuilder();
        rowText.append("资产名称:").append(nullSafe(row.getAssetName())).append("\n");
        rowText.append("分类:").append(nullSafe(row.getCategoryText())).append("\n");
        rowText.append("购入日期:").append(row.getPurchaseDate()).append("\n");
        rowText.append("位置:").append(nullSafe(row.getLocationText()));
        AiRecognizedAssetFields fields = aiRecognitionService.enhanceExcelRow(rowText.toString());
        if (fields == null) {
            return;
        }
        if ((row.getAssetName() == null || row.getAssetName().isBlank()) && fields.getAssetName() != null) {
            row.setAssetName(fields.getAssetName());
        }
        if ((row.getCategoryText() == null || row.getCategoryText().isBlank()) && fields.getCategoryText() != null) {
            row.setCategoryText(fields.getCategoryText());
        }
        if (row.getPurchaseDate() == null) {
            row.setPurchaseDate(aiRecognitionService.parseDate(fields.getPurchaseDate()));
        }
        if (row.getEnabledDate() == null) {
            row.setEnabledDate(aiRecognitionService.parseDate(fields.getEnabledDate()));
        }
        if ((row.getAssetDescription() == null || row.getAssetDescription().isBlank()) && fields.getAssetDescription() != null) {
            row.setAssetDescription(fields.getAssetDescription());
        }
    }

    private void applyRecognizedFields(AssetImportItem item, AiRecognizedAssetFields fields) {
        if (fields == null) {
            return;
        }
        item.setAssetName(trim(fields.getAssetName()));
        item.setCategoryText(trim(fields.getCategoryText()));
        item.setAssetCategoryId(categorySemanticMatch.matchCategoryId(fields.getCategoryText(), SecurityUtils.current().userId()));
        item.setPurchaseDate(aiRecognitionService.parseDate(fields.getPurchaseDate()));
        item.setEnabledDate(aiRecognitionService.parseDate(fields.getEnabledDate()));
        item.setAssetDescription(trim(fields.getAssetDescription()));
        item.setDuplicateHint(buildDuplicateHint(item.getAssetName(), item.getCategoryText(), item.getAssetCategoryId()));
    }

    private String imageExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        }
        return "jpg";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
