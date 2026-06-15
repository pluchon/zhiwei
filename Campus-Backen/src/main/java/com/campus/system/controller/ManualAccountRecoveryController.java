package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.ManualRecoveryCreateDTO;
import com.campus.system.dto.ManualRecoveryPhoneVerifyDTO;
import com.campus.system.dto.ManualRecoveryQueryDTO;
import com.campus.system.dto.ManualRecoveryReviewDTO;
import com.campus.system.service.interfaces.ManualAccountRecoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 账号人工恢复接口
@RestController
public class ManualAccountRecoveryController {

    @Autowired
    private ManualAccountRecoveryService service;

    /**
     * 管理员分页查询人工恢复申请
     */
    @GetMapping("/admin/manual-account-recovery")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> search(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, ManualRecoveryQueryDTO query) {
        return ApiResponse.ok(service.search(pageNum, pageSize, query));
    }

    /**
     * 管理员查询人工恢复申请详情
     */
    @GetMapping("/admin/manual-account-recovery/{recoveryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> detail(@PathVariable Long recoveryId) {
        return ApiResponse.ok(service.detail(recoveryId));
    }

    /**
     * 管理员创建人工恢复申请
     */
    @PostMapping("/admin/manual-account-recovery")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> create(@RequestBody ManualRecoveryCreateDTO body) {
        return ApiResponse.ok(service.create(body));
    }

    /**
     * 发起管理员撤销待复核申请
     */
    @PostMapping("/admin/manual-account-recovery/{recoveryId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> cancel(@PathVariable Long recoveryId) {
        service.cancel(recoveryId);
        return ApiResponse.ok(null);
    }

    /**
     * 复核管理员审批人工恢复申请
     */
    @PostMapping("/admin/manual-account-recovery/{recoveryId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> review(@PathVariable Long recoveryId, @RequestBody ManualRecoveryReviewDTO body) {
        return ApiResponse.ok(service.review(recoveryId, body));
    }

    /**
     * 用户查询可验证的人工恢复申请信息
     */
    @GetMapping("/manual-account-recovery/{recoveryId}/verify-info")
    public ApiResponse<?> verifyInfo(@PathVariable Long recoveryId) {
        return ApiResponse.ok(service.verifyInfo(recoveryId));
    }

    /**
     * 用户完成新手机号验证并换绑
     */
    @PostMapping("/manual-account-recovery/{recoveryId}/verify-phone")
    public ApiResponse<?> verifyPhone(@PathVariable Long recoveryId, @RequestBody ManualRecoveryPhoneVerifyDTO body) {
        service.verifyPhone(recoveryId, body);
        return ApiResponse.ok(null);
    }
}
