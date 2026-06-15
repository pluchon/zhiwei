package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.AssetEditDTO;
import com.campus.system.dto.AssetQueryDTO;
import com.campus.system.dto.AssetStatusChangeDTO;
import com.campus.system.service.interfaces.AssetService;
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
import org.springframework.web.multipart.MultipartFile;

// 资产台账接口
@RestController
@RequestMapping("/assets")
public class AssetController {

    @Autowired
    private AssetService service;

    /**
     * 分页查询资产列表
     */
    @GetMapping
    public ApiResponse<?> search(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, AssetQueryDTO query) {
        return ApiResponse.ok(service.search(pageNum, pageSize, query));
    }

    /**
     * 查询资产详情
     */
    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    /**
     * 查询资产维修历史
     */
    @GetMapping("/{id}/repair-history")
    public ApiResponse<?> repairHistory(@PathVariable Long id, @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.repairHistory(id, pageNum, pageSize));
    }

    /**
     * 管理员新增资产
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> create(@RequestBody AssetEditDTO body) {
        return ApiResponse.ok(service.create(body));
    }

    /**
     * 管理员修改资产
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody AssetEditDTO body) {
        return ApiResponse.ok(service.update(id, body));
    }

    /**
     * 管理员变更资产状态
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> changeStatus(@PathVariable Long id, @RequestBody AssetStatusChangeDTO body) {
        service.changeStatus(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员逻辑删除资产
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id, @RequestParam Integer version) {
        service.delete(id, version);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员恢复已删除资产
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> restore(@PathVariable Long id) {
        service.restore(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员上传资产图片
     */
    @PostMapping("/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.ok(service.uploadImage(file));
    }
}
