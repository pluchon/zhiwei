package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.util.NameSortUtil;
import com.campus.system.converter.EntityVOConverter;
import com.campus.system.dto.BuildingEditDTO;
import com.campus.system.dto.CampusEditDTO;
import com.campus.system.entity.Building;
import com.campus.system.entity.Campus;
import com.campus.system.entity.RepairOrder;
import com.campus.system.mapper.BuildingMapper;
import com.campus.system.mapper.CampusMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.entity.LocationSnapshot;
import com.campus.system.service.interfaces.LocationService;
import com.campus.system.vo.BuildingOptionVO;
import com.campus.system.vo.BuildingVO;
import com.campus.system.vo.CampusVO;
import com.campus.system.vo.LocationOptionVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 校区与楼栋维护业务实现。
@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private CampusMapper campusMapper;

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private RepairOrderMapper orderMapper;

    @Override
    public List<LocationOptionVO> selectionOptions() {
        List<Campus> campuses = campusMapper.selectList(Wrappers.<Campus>lambdaQuery().eq(Campus::getDeleteState, 0)
                .eq(Campus::getStatus, 0).orderByAsc(Campus::getNameSortKey, Campus::getCampusName, Campus::getCampusId));
        List<LocationOptionVO> result = new ArrayList<>();
        for (Campus campus : campuses) {
            LocationOptionVO option = new LocationOptionVO();
            option.setCampusId(campus.getCampusId());
            option.setCampusName(campus.getCampusName());
            option.setDescription(campus.getDescription());
            List<Building> buildings = buildingMapper.selectList(Wrappers.<Building>lambdaQuery()
                    .eq(Building::getCampusId, campus.getCampusId()).eq(Building::getDeleteState, 0)
                    .eq(Building::getStatus, 0).orderByAsc(Building::getNameSortKey, Building::getBuildingName, Building::getBuildingId));
            List<BuildingOptionVO> buildingOptions = new ArrayList<>();
            for (Building building : buildings) {
                BuildingOptionVO buildingOption = new BuildingOptionVO();
                buildingOption.setBuildingId(building.getBuildingId());
                buildingOption.setBuildingName(building.getBuildingName());
                buildingOption.setDescription(building.getDescription());
                buildingOptions.add(buildingOption);
            }
            option.setBuildings(buildingOptions);
            result.add(option);
        }
        return result;
    }

    @Override
    public List<CampusVO> adminCampuses() {
        return EntityVOConverter.toCampusVOList(campusMapper.selectList(Wrappers.<Campus>lambdaQuery()
                .orderByAsc(Campus::getNameSortKey, Campus::getCampusName, Campus::getCampusId)));
    }

    @Override
    public List<BuildingVO> adminBuildings(Long campusId) {
        return EntityVOConverter.toBuildingVOList(buildingMapper.selectList(Wrappers.<Building>lambdaQuery().eq(Building::getCampusId, campusId)
                .orderByAsc(Building::getNameSortKey, Building::getBuildingName, Building::getBuildingId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampusVO createCampus(CampusEditDTO body) {
        Campus campus = new Campus();
        applyCampusName(campus, required(body.getCampusName(), "campusName"));
        campus.setDescription(trim(body.getDescription()));
        campus.setStatus(0);
        campusMapper.insert(campus);
        return EntityVOConverter.toCampusVO(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampusVO updateCampus(Long id, CampusEditDTO body) {
        Campus campus = requireCampus(id);
        if (body.getCampusName() != null) {
            applyCampusName(campus, required(body.getCampusName(), "campusName"));
        }
        if (body.getDescription() != null) {
            campus.setDescription(trim(body.getDescription()));
        }
        campusMapper.updateById(campus);
        return EntityVOConverter.toCampusVO(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableCampus(Long id) {
        Campus campus = requireCampus(id);
        campus.setStatus(0);
        campusMapper.updateById(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableCampus(Long id) {
        Campus campus = requireCampus(id);
        long activeBuildings = buildingMapper.selectCount(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, id).eq(Building::getDeleteState, 0).eq(Building::getStatus, 0));
        if (activeBuildings > 0) {
            throw BusinessException.conflict("校区下仍存在启用楼栋，请先处理所属楼栋");
        }
        campus.setStatus(1);
        campusMapper.updateById(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampus(Long id) {
        Campus campus = requireCampus(id);
        if (campus.getDeleteState() != null && campus.getDeleteState() == 1) {
          throw BusinessException.conflict("校区已删除");
        }
        long activeBuildings = buildingMapper.selectCount(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, id).eq(Building::getDeleteState, 0).eq(Building::getStatus, 0));
        if (activeBuildings > 0) {
            throw BusinessException.conflict("校区下仍存在启用楼栋，请先处理所属楼栋");
        }
        assertNoBlockingOrdersForCampus(id);
        campus.setDeleteState(1);
        campusMapper.updateById(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreCampus(Long id) {
        Campus campus = campusMapper.selectById(id);
        if (campus == null || campus.getDeleteState() == null || campus.getDeleteState() != 1) {
            throw BusinessException.conflict("仅已删除校区可恢复");
        }
        assertNameAvailable(campus.getNormalizedName(), id);
        campus.setDeleteState(0);
        campus.setStatus(0);
        campusMapper.updateById(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingVO createBuilding(BuildingEditDTO body) {
        Long campusId = required(body.getCampusId(), "campusId");
        requireActiveCampus(campusId);
        Building building = new Building();
        building.setCampusId(campusId);
        applyBuildingName(building, required(body.getBuildingName(), "buildingName"));
        building.setDescription(trim(body.getDescription()));
        building.setStatus(0);
        buildingMapper.insert(building);
        return EntityVOConverter.toBuildingVO(building);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingVO updateBuilding(Long id, BuildingEditDTO body) {
        Building building = requireBuilding(id);
        if (body.getCampusId() != null && !body.getCampusId().equals(building.getCampusId())) {
            requireActiveCampus(body.getCampusId());
            building.setCampusId(body.getCampusId());
        }
        if (body.getBuildingName() != null) {
            applyBuildingName(building, required(body.getBuildingName(), "buildingName"));
        }
        if (body.getDescription() != null) {
            building.setDescription(trim(body.getDescription()));
        }
        buildingMapper.updateById(building);
        return EntityVOConverter.toBuildingVO(building);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBuilding(Long id) {
        Building building = requireBuilding(id);
        requireActiveCampus(building.getCampusId());
        building.setStatus(0);
        buildingMapper.updateById(building);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableBuilding(Long id) {
        Building building = requireBuilding(id);
        building.setStatus(1);
        buildingMapper.updateById(building);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long id) {
        Building building = requireBuilding(id);
        if (building.getDeleteState() != null && building.getDeleteState() == 1) {
            throw BusinessException.conflict("楼栋已删除");
        }
        assertNoBlockingOrdersForBuilding(id);
        building.setDeleteState(1);
        buildingMapper.updateById(building);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreBuilding(Long id) {
        Building building = buildingMapper.selectById(id);
        if (building == null || building.getDeleteState() == null || building.getDeleteState() != 1) {
            throw BusinessException.conflict("仅已删除楼栋可恢复");
        }
        requireActiveCampus(building.getCampusId());
        assertBuildingNameAvailable(building.getCampusId(), building.getNormalizedName(), id);
        building.setDeleteState(0);
        building.setStatus(0);
        buildingMapper.updateById(building);
    }

    @Override
    public LocationSnapshot resolveSnapshot(Long campusId, Long buildingId, String locationDetail) {
        Campus campus = campusMapper.selectById(required(campusId, "campusId"));
        if (campus == null || campus.getDeleteState() != 0 || campus.getStatus() != 0) {
            throw BusinessException.badRequest("请选择启用的校区");
        }
        if (buildingId == null && (locationDetail == null || locationDetail.isBlank())) {
            throw BusinessException.badRequest("未选择楼栋时必须填写具体位置描述");
        }
        LocationSnapshot snapshot = new LocationSnapshot();
        snapshot.setCampusId(campus.getCampusId());
        snapshot.setCampusName(campus.getCampusName());
        snapshot.setCampusDescription(campus.getDescription());
        if (buildingId != null) {
            Building building = buildingMapper.selectById(buildingId);
            if (building == null || building.getDeleteState() != 0 || building.getStatus() != 0) {
                throw BusinessException.badRequest("请选择启用且属于当前校区的楼栋");
            }
            if (!building.getCampusId().equals(campusId)) {
                throw BusinessException.badRequest("楼栋不属于所选校区");
            }
            snapshot.setBuildingId(building.getBuildingId());
            snapshot.setBuildingName(building.getBuildingName());
            snapshot.setBuildingDescription(building.getDescription());
        }
        return snapshot;
    }

    private void applyCampusName(Campus campus, String name) {
        String trimmed = NameSortUtil.trimName(name);
        String normalized = NameSortUtil.normalizeName(trimmed);
        assertNameAvailable(normalized, campus.getCampusId());
        campus.setCampusName(trimmed);
        campus.setNormalizedName(normalized);
        campus.setNameSortKey(NameSortUtil.sortKey(trimmed));
    }

    private void applyBuildingName(Building building, String name) {
        String trimmed = NameSortUtil.trimName(name);
        String normalized = NameSortUtil.normalizeName(trimmed);
        assertBuildingNameAvailable(building.getCampusId(), normalized, building.getBuildingId());
        building.setBuildingName(trimmed);
        building.setNormalizedName(normalized);
        building.setNameSortKey(NameSortUtil.sortKey(trimmed));
    }

    private void assertNameAvailable(String normalized, Long excludeId) {
        Campus existing = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getNormalizedName, normalized));
        if (existing != null && (!existing.getCampusId().equals(excludeId))) {
            throw BusinessException.conflict("校区名称已存在");
        }
    }

    private void assertBuildingNameAvailable(Long campusId, String normalized, Long excludeId) {
        Building existing = buildingMapper.selectOne(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, campusId).eq(Building::getNormalizedName, normalized));
        if (existing != null && (!existing.getBuildingId().equals(excludeId))) {
            throw BusinessException.conflict("同校区下楼栋名称已存在");
        }
    }

    private void assertNoBlockingOrdersForCampus(Long campusId) {
        long count = orderMapper.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getCampusId, campusId)
                .eq(RepairOrder::getDeleteState, 0).notIn(RepairOrder::getStatus, RepairStatus.COMPLETED.getCode(), RepairStatus.CLOSED.getCode()));
        if (count > 0) {
            throw BusinessException.conflict("存在非终态关联工单，不能删除校区");
        }
    }

    private void assertNoBlockingOrdersForBuilding(Long buildingId) {
        long count = orderMapper.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getBuildingId, buildingId)
                .eq(RepairOrder::getDeleteState, 0).notIn(RepairOrder::getStatus, RepairStatus.COMPLETED.getCode(), RepairStatus.CLOSED.getCode()));
        if (count > 0) {
            throw BusinessException.conflict("存在非终态关联工单，不能删除楼栋");
        }
    }

    private Campus requireCampus(Long id) {
        Campus campus = campusMapper.selectById(id);
        if (campus == null || campus.getDeleteState() == 1) {
            throw BusinessException.notFound("校区不存在");
        }
        return campus;
    }

    private Building requireBuilding(Long id) {
        Building building = buildingMapper.selectById(id);
        if (building == null || building.getDeleteState() == 1) {
            throw BusinessException.notFound("楼栋不存在");
        }
        return building;
    }

    private void requireActiveCampus(Long campusId) {
        Campus campus = campusMapper.selectById(campusId);
        if (campus == null || campus.getDeleteState() != 0 || campus.getStatus() != 0) {
            throw BusinessException.badRequest("所属校区必须处于启用状态");
        }
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
}
