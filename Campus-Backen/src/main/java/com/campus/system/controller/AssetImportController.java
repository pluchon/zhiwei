package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.AssetImportBatchQueryDTO;
import com.campus.system.dto.AssetImportConfirmDTO;
import com.campus.system.dto.AssetImportItemUpdateDTO;
import com.campus.system.service.interfaces.AssetImportService;
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

// 资产批量导入接口
@RestController
@RequestMapping("/admin/asset-import")
@PreAuthorize("hasRole('ADMIN')")
public class AssetImportController {

    @Autowired
    private AssetImportService service;

    /**
     * 上传 Excel 并创建导入批次
     */
    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(file));
    }

    /**
     * 上传图片并创建图片导入批次
     */
    @PostMapping("/upload-images")
    public ApiResponse<?> uploadImages(@RequestParam("files") MultipartFile[] files) {
        return ApiResponse.ok(service.uploadImages(files));
    }

    /**
     * 分页查询导入批次列表
     */
    @GetMapping("/batches")
    public ApiResponse<?> listBatches(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, AssetImportBatchQueryDTO query) {
        return ApiResponse.ok(service.listBatches(pageNum, pageSize, query));
    }

    /**
     * 查询导入批次详情
     */
    @GetMapping("/batches/{batchId}")
    public ApiResponse<?> getBatch(@PathVariable Long batchId) {
        return ApiResponse.ok(service.getBatch(batchId));
    }

    /**
     * 分页查询批次内资产卡片
     */
    @GetMapping("/batches/{batchId}/items")
    public ApiResponse<?> listBatchItems(@PathVariable Long batchId, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.listBatchItems(batchId, status, pageNum, pageSize));
    }

    /**
     * 编辑待审核资产卡片
     */
    @PutMapping("/items/{itemId}")
    public ApiResponse<?> updateItem(@PathVariable Long itemId, @RequestBody AssetImportItemUpdateDTO body) {
        return ApiResponse.ok(service.updateItem(itemId, body));
    }

    /**
     * 忽略待审核资产卡片
     */
    @PostMapping("/items/{itemId}/ignore")
    public ApiResponse<?> ignoreItem(@PathVariable Long itemId) {
        service.ignoreItem(itemId);
        return ApiResponse.ok(null);
    }

    /**
     * 确认单条资产卡片入库
     */
    @PostMapping("/items/{itemId}/confirm")
    public ApiResponse<?> confirmItem(@PathVariable Long itemId) {
        return ApiResponse.ok(service.confirmItem(itemId));
    }

    /**
     * 批量确认资产卡片入库
     */
    @PostMapping("/items/confirm")
    public ApiResponse<?> confirmBatch(@RequestBody AssetImportConfirmDTO body) {
        return ApiResponse.ok(service.confirmBatch(body));
    }

    /**
     * 删除导入批次及未入库卡片
     */
    @PostMapping("/batches/{batchId}/delete")
    public ApiResponse<?> deleteBatch(@PathVariable Long batchId) {
        service.deleteBatch(batchId);
        return ApiResponse.ok(null);
    }
}
