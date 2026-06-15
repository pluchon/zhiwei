package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.AdminArbitrateDTO;
import com.campus.system.dto.AdminCreateUserDTO;
import com.campus.system.dto.AdminDispatchDTO;
import com.campus.system.dto.AdminOrderActionDTO;
import com.campus.system.dto.AdminUpdateUserDTO;
import com.campus.system.service.interfaces.AdminDashboardService;
import com.campus.system.dto.RepairCapabilityDTO;
import com.campus.system.dto.RepairCategoryDTO;
import com.campus.system.dto.SysDictDataDTO;
import com.campus.system.service.interfaces.AdminService;
import com.campus.system.service.interfaces.RepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.dto.UserQueryDTO;
import com.campus.system.service.interfaces.RepairOrderExportService;
import com.campus.system.service.interfaces.UserExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 管理员后台
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService service;

    @Autowired
    private AdminDashboardService dashboardService;

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private RepairOrderExportService exportService;

    @Autowired
    private UserExportService userExportService;

    /**
     * 分页查询平台所有用户列表（用于后台用户管理）
    */
    @GetMapping("/users")
    public ApiResponse<?> users(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, UserQueryDTO query) {
        return ApiResponse.ok(service.users(pageNum, pageSize, query));
    }

    /**
     * 导出当前筛选条件下或已选中的用户
     */
    @GetMapping("/users/export")
    public void exportUsers(UserQueryDTO query, HttpServletResponse response) {
        userExportService.export(query, response);
    }

    /**
     * 在后台直接创建一个新用户账号
    */
    @PostMapping("/users")
    public ApiResponse<?> createUser(@RequestBody AdminCreateUserDTO body) {
        return ApiResponse.ok(service.createUser(body));
    }

    /**
     * 修改指定用户的资料、状态或角色
    */
    @PutMapping("/users/{id}")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody AdminUpdateUserDTO body) {
        return ApiResponse.ok(service.updateUser(id, body));
    }

    /**
     * 查询所有维修故障分类（后台大屏/列表使用）
    */
    @GetMapping("/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.ok(service.categories());
    }

    /**
     * 新增一个维修故障分类
    */
    @PostMapping("/categories")
    public ApiResponse<?> categoryCreate(@RequestBody RepairCategoryDTO body) {
        return ApiResponse.ok(service.createCategory(body));
    }

    /**
     * 修改维修故障分类的信息或启禁用状态
    */
    @PutMapping("/categories/{id}")
    public ApiResponse<?> categoryUpdate(@PathVariable Long id, @RequestBody RepairCategoryDTO body) {
        return ApiResponse.ok(service.updateCategory(id, body));
    }

    /**
     * 查询所有维修师傅的能力（擅长维修 of 分类）列表
    */
    @GetMapping("/capabilities")
    public ApiResponse<?> capabilities() {
        return ApiResponse.ok(service.capabilities());
    }

    /**
     * 为某位维修师傅新增一项维修能力
    */
    @PostMapping("/capabilities")
    public ApiResponse<?> capabilityCreate(@RequestBody RepairCapabilityDTO body) {
        return ApiResponse.ok(service.createCapability(body));
    }

    /**
     * 移除维修师傅的某项维修能力
    */
    @DeleteMapping("/capabilities/{id}")
    public ApiResponse<?> capabilityDelete(@PathVariable Long id) {
        service.deleteCapability(id);
        return ApiResponse.ok(null);
    }

    /**
     * 查询所有的数据字典类型（如性别配置、工单状态配置）
    */
    @GetMapping("/dicts/types")
    public ApiResponse<?> dictTypes() {
        return ApiResponse.ok(service.dictTypes());
    }

    /**
     * 根据字典类型，查询其下包含的具体字典数据项
    */
    @GetMapping("/dicts/data")
    public ApiResponse<?> dictData(@RequestParam(required = false) String dictType) {
        return ApiResponse.ok(service.dictData(dictType));
    }

    /**
     * 新增一条字典数据项
    */
    @PostMapping("/dicts/data")
    public ApiResponse<?> dictCreate(@RequestBody SysDictDataDTO body) {
        return ApiResponse.ok(service.createDictData(body));
    }

    /**
     * 修改字典数据项的值或状态
    */
    @PutMapping("/dicts/data/{id}")
    public ApiResponse<?> dictUpdate(@PathVariable Long id, @RequestBody SysDictDataDTO body) {
        return ApiResponse.ok(service.updateDictData(id, body));
    }

    /**
     * 分页查询后台的登录审计日志
    */
    @GetMapping("/audit/login-logs")
    public ApiResponse<?> loginLogs(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.loginLogs(pageNum, pageSize));
    }

    /**
     * 分页查询后台管理员的敏感操作审计日志
    */
    @GetMapping("/audit/operation-logs")
    public ApiResponse<?> operationLogs(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.operationLogs(pageNum, pageSize));
    }

    /**
     * 管理员驳回工单（例如恶意报修或者信息不全）
    */
    @PostMapping("/orders/{id}/reject")
    public ApiResponse<?> reject(@PathVariable Long id, @RequestBody AdminOrderActionDTO body) {
        service.rejectOrder(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员强制关闭工单
    */
    @PostMapping("/orders/{id}/close")
    public ApiResponse<?> close(@PathVariable Long id, @RequestBody AdminOrderActionDTO body) {
        service.closeOrder(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员仲裁纠纷工单（当报修人和维修师傅意见不一致时，由管理员强行变更为指定状态）
    */
    @PostMapping("/orders/{id}/arbitrate")
    public ApiResponse<?> arbitrate(@PathVariable Long id, @RequestBody AdminArbitrateDTO body) {
        service.arbitrateOrder(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 获取报修数据看板统计信息
    */
    @GetMapping("/repair-dashboard")
    public ApiResponse<?> repairDashboard(@RequestParam(required = false) Integer rangeDays) {
        return ApiResponse.ok(dashboardService.dashboard(rangeDays));
    }

    /**
     * 导出当前筛选条件下的工单
     */
    @GetMapping("/orders/export")
    public void exportOrders(RepairOrderQueryDTO query, HttpServletResponse response) {
        exportService.export(query, response);
    }

    /**
     * 获取指定工单的可派单维修师傅候选人列表
    */
    @GetMapping("/orders/{id}/dispatch-candidates")
    public ApiResponse<?> dispatchCandidates(@PathVariable Long id) {
        return ApiResponse.ok(repairOrderService.dispatchCandidates(id));
    }

    /**
     * 管理员手动派单给指定的维修师傅
    */
    @PostMapping("/orders/{id}/dispatch")
    public ApiResponse<?> dispatch(@PathVariable Long id, @RequestBody AdminDispatchDTO body) {
        repairOrderService.adminDispatch(id, body);
        return ApiResponse.ok(null);
    }
}
