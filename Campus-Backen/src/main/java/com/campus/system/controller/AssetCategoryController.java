package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.AssetCategoryDTO;
import com.campus.system.dto.AssetCategoryQueryDTO;
import com.campus.system.service.interfaces.AssetCategoryExportService;
import com.campus.system.service.interfaces.AssetCategoryService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 资产分类接口
@RestController
@RequestMapping("/asset/categories")
public class AssetCategoryController {

    @Autowired
    private AssetCategoryService service;

    @Autowired
    private AssetCategoryExportService exportService;

    /**
     * 查询启用中的资产分类列表
     */
    @GetMapping("/enabled")
    public ApiResponse<?> enabled() {
        return ApiResponse.ok(service.enabledList());
    }

    /**
     * 管理员分页查询资产分类
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, AssetCategoryQueryDTO query) {
        return ApiResponse.ok(service.adminList(pageNum, pageSize, query));
    }

    /**
     * 导出当前筛选条件下或已选中的资产分类
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(AssetCategoryQueryDTO query, HttpServletResponse response) {
        exportService.export(query, response);
    }

    /**
     * 管理员新增资产分类
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> create(@RequestBody AssetCategoryDTO body) {
        return ApiResponse.ok(service.create(body));
    }

    /**
     * 管理员修改资产分类
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody AssetCategoryDTO body) {
        return ApiResponse.ok(service.update(id, body));
    }

    /**
     * 管理员停用资产分类
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> disable(@PathVariable Long id) {
        service.disable(id);
        return ApiResponse.ok(null);
    }
}
