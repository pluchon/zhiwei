package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.RepairOrderLinkConfirmDTO;
import com.campus.system.service.interfaces.RepairOrderAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 管理员工单 AI 辅助接口
@RestController
@RequestMapping("/admin/repair-orders/ai")
@PreAuthorize("hasRole('ADMIN')")
public class RepairOrderAiController {

    @Autowired
    private RepairOrderAiService service;

    /**
     * 加载疑似重复工单的 AI 详情与关联推荐
     */
    @GetMapping("/{orderId}/duplicate-detail")
    public ApiResponse<?> duplicateDetail(@PathVariable Long orderId) {
        return ApiResponse.ok(service.loadDuplicateDetail(orderId));
    }

    /**
     * 确认 AI 推荐的工单关联
     */
    @PostMapping("/links/confirm")
    public ApiResponse<?> confirmLink(@RequestBody RepairOrderLinkConfirmDTO body) {
        service.confirmLink(body);
        return ApiResponse.ok(null);
    }

    /**
     * 解除 AI 推荐的工单关联
     */
    @PostMapping("/links/{linkId}/remove")
    public ApiResponse<?> removeLink(@PathVariable Long linkId) {
        service.removeLink(linkId);
        return ApiResponse.ok(null);
    }

    /**
     * 派单辅助 AI 文字分析
     */
    @PostMapping("/{orderId}/dispatch-analysis")
    public ApiResponse<?> dispatchAnalysis(@PathVariable Long orderId) {
        return ApiResponse.ok(service.analyzeDispatch(orderId));
    }
}
