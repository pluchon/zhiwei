package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.BuildingEditDTO;
import com.campus.system.dto.CampusEditDTO;
import com.campus.system.service.interfaces.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 校区与楼栋
@RestController
public class LocationController {

    @Autowired
    private LocationService locationService;

    /**
     * 获取供报修时选择的校区及所属楼栋级联选项列表
    */
    @GetMapping("/repair/locations/options")
    public ApiResponse<?> options() {
        return ApiResponse.ok(locationService.selectionOptions());
    }

    /**
     * 管理员获取所有校区列表（包含禁用的校区）
    */
    @GetMapping("/admin/locations/campuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> adminCampuses() {
        return ApiResponse.ok(locationService.adminCampuses());
    }

    /**
     * 管理员获取指定校区下的所有楼栋列表
    */
    @GetMapping("/admin/locations/campuses/{campusId}/buildings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> adminBuildings(@PathVariable Long campusId) {
        return ApiResponse.ok(locationService.adminBuildings(campusId));
    }

    /**
     * 管理员新增一个校区
    */
    @PostMapping("/admin/locations/campuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createCampus(@RequestBody CampusEditDTO body) {
        return ApiResponse.ok(locationService.createCampus(body));
    }

    /**
     * 管理员修改校区信息
    */
    @PutMapping("/admin/locations/campuses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateCampus(@PathVariable Long id, @RequestBody CampusEditDTO body) {
        return ApiResponse.ok(locationService.updateCampus(id, body));
    }

    /**
     * 管理员启用指定的校区
    */
    @PostMapping("/admin/locations/campuses/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> enableCampus(@PathVariable Long id) {
        locationService.enableCampus(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员禁用指定的校区
    */
    @PostMapping("/admin/locations/campuses/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> disableCampus(@PathVariable Long id) {
        locationService.disableCampus(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员软删除指定的校区
    */
    @PostMapping("/admin/locations/campuses/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteCampus(@PathVariable Long id) {
        locationService.deleteCampus(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员恢复已软删除的校区
    */
    @PostMapping("/admin/locations/campuses/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> restoreCampus(@PathVariable Long id) {
        locationService.restoreCampus(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员新增一个楼栋
    */
    @PostMapping("/admin/locations/buildings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createBuilding(@RequestBody BuildingEditDTO body) {
        return ApiResponse.ok(locationService.createBuilding(body));
    }

    /**
     * 管理员修改楼栋信息
    */
    @PutMapping("/admin/locations/buildings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateBuilding(@PathVariable Long id, @RequestBody BuildingEditDTO body) {
        return ApiResponse.ok(locationService.updateBuilding(id, body));
    }

    /**
     * 管理员启用指定的楼栋
    */
    @PostMapping("/admin/locations/buildings/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> enableBuilding(@PathVariable Long id) {
        locationService.enableBuilding(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员禁用指定的楼栋
    */
    @PostMapping("/admin/locations/buildings/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> disableBuilding(@PathVariable Long id) {
        locationService.disableBuilding(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员软删除指定的楼栋
    */
    @PostMapping("/admin/locations/buildings/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteBuilding(@PathVariable Long id) {
        locationService.deleteBuilding(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员恢复已软删除的楼栋
    */
    @PostMapping("/admin/locations/buildings/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> restoreBuilding(@PathVariable Long id) {
        locationService.restoreBuilding(id);
        return ApiResponse.ok(null);
    }
}
