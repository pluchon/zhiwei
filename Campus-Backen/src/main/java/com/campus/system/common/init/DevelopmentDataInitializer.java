package com.campus.system.common.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.util.NameSortUtil;
import com.campus.system.entity.Building;
import com.campus.system.entity.Campus;
import com.campus.system.entity.RepairCapability;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.BuildingMapper;
import com.campus.system.mapper.CampusMapper;
import com.campus.system.mapper.RepairCapabilityMapper;
import com.campus.system.mapper.SysUserMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 系统启动的时候自动初始化种子数据
@Component
@Profile("!prod")
public class DevelopmentDataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper users;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RepairCapabilityMapper capabilities;

    @Autowired
    private CampusMapper campusMapper;

    @Autowired
    private BuildingMapper buildingMapper;

    @Override
    public void run(String... args) {
        seed("admin", 4L, "李工为", "13800000001");
        seed("student", 1L, "张三", "13800000002");
        seed("teacher", 2L, "王春", "13800000003");
        seed("repairer", 3L, "段凯金", "13800000004");
        SysUser repairer = users.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserNo, "repairer"));
        for (long categoryId = 1; categoryId <= 3; categoryId++) {
            if (capabilities.selectCount(Wrappers.<RepairCapability>lambdaQuery()
                      .eq(RepairCapability::getRepairerId, repairer.getUserId()).eq(RepairCapability::getCategoryId, categoryId)) == 0) {
                RepairCapability capability = new RepairCapability();
                capability.setRepairerId(repairer.getUserId());
                capability.setCategoryId(categoryId);
                capabilities.insert(capability);
            }
        }
        seedLocations();
    }

    private void seedLocations() {
        Long mainCampusId = seedCampusIfAbsent("主校区", "日常教学区");
        Long eastCampusId = seedCampusIfAbsent("东校区", "实践基地");
        seedBuildingIfAbsent(mainCampusId, "教学楼A", "主校区北侧教学楼");
        seedBuildingIfAbsent(mainCampusId, "宿舍楼1号", "学生宿舍区");
        seedBuildingIfAbsent(eastCampusId, "体育馆", "东校区综合体育馆");
    }

    private Long seedCampusIfAbsent(String name, String description) {
        String normalized = NameSortUtil.normalizeName(name);
        Campus existing = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getNormalizedName, normalized));
        if (existing != null) {
            return existing.getCampusId();
        }
        Campus campus = new Campus();
        campus.setCampusName(NameSortUtil.trimName(name));
        campus.setNormalizedName(normalized);
        campus.setNameSortKey(NameSortUtil.sortKey(name));
        campus.setDescription(description);
        campus.setStatus(0);
        campusMapper.insert(campus);
        return campus.getCampusId();
    }

    private void seedBuildingIfAbsent(Long campusId, String name, String description) {
        String normalized = NameSortUtil.normalizeName(name);
        Building existing = buildingMapper.selectOne(Wrappers.<Building>lambdaQuery().eq(Building::getCampusId, campusId).eq(Building::getNormalizedName, normalized));
        if (existing != null) {
            return;
        }
        Building building = new Building();
        building.setCampusId(campusId);
        building.setBuildingName(NameSortUtil.trimName(name));
        building.setNormalizedName(normalized);
        building.setNameSortKey(NameSortUtil.sortKey(name));
        building.setDescription(description);
        building.setStatus(0);
        buildingMapper.insert(building);
    }

    private void seed(String userNo, Long roleId, String name, String phone) {
        if (users.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserNo, userNo)) > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUserNo(userNo);
        user.setRealName(name);
        user.setNickName(name);
        user.setRoleId(roleId);
        user.setPhoneNumber(phone);
        user.setPassword(encoder.encode("husa123456"));
        user.setActivationStatus(1);
        user.setAccountStatus(0);
        user.setSecurityStamp(UUID.randomUUID().toString().replace("-", ""));
        user.setPhoneConfirmRequired(0);
        users.insert(user);
    }
}
