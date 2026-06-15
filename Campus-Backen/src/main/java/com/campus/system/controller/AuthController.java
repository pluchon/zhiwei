package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.ActivationCompleteDTO;
import com.campus.system.dto.ActivationStartDTO;
import com.campus.system.dto.CaptchaChallengeDTO;
import com.campus.system.dto.CaptchaTicketDTO;
import com.campus.system.dto.ChangeContactDTO;
import com.campus.system.dto.ChangePasswordDTO;
import com.campus.system.dto.PasswordLoginDTO;
import com.campus.system.dto.PhoneLoginDTO;
import com.campus.system.dto.RecoveryCompleteDTO;
import com.campus.system.dto.RecoveryVerifyDTO;
import com.campus.system.dto.VerificationCodeDTO;
import com.campus.system.service.interfaces.AuthService;
import com.campus.system.service.interfaces.PortalSummaryService;
import com.campus.system.service.interfaces.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 认证与授权
@RestController
public class AuthController {

    @Autowired
    private AuthService service;

    @Autowired
    private UserProfileService profileService;

    @Autowired
    private PortalSummaryService portalSummaryService;

    /**
     * 登录页公开概览
     */
    @GetMapping("/auth/portal/summary")
    public ApiResponse<?> portalSummary() {
        return ApiResponse.ok(portalSummaryService.summary());
    }

    /**
     * 获取人机验证（验证码）挑战数据
    */
    @PostMapping("/auth/captcha/challenge")
    public ApiResponse<?> challenge(@RequestBody CaptchaChallengeDTO body) {
        return ApiResponse.ok(service.captchaChallenge(body));
    }

    /**
     * 校验人机验证结果并生成票据（Ticket）
    */
    @PostMapping("/auth/captcha/ticket")
    public ApiResponse<?> ticket(@RequestBody CaptchaTicketDTO body) {
        return ApiResponse.ok(service.captchaTicket(body));
    }

    /**
     * 发送短信或邮箱验证码
    */
    @PostMapping("/auth/verification-codes")
    public ApiResponse<?> code(@RequestBody VerificationCodeDTO body) {
        return ApiResponse.ok(service.sendCode(body));
    }

    /**
     * 账号密码登录接口
    */
    @PostMapping("/auth/login/password")
    public ApiResponse<?> password(@RequestBody PasswordLoginDTO body) {
        return ApiResponse.ok(service.loginPassword(body));
    }

    /**
     * 手机验证码快捷登录接口
    */
    @PostMapping("/auth/login/phone")
    public ApiResponse<?> phone(@RequestBody PhoneLoginDTO body) {
        return ApiResponse.ok(service.loginPhone(body));
    }

    /**
     * 开始账号激活流程（发送激活验证）
    */
    @PostMapping("/auth/activation/start")
    public ApiResponse<?> activationStart(@RequestBody ActivationStartDTO body) {
        return ApiResponse.ok(service.activationStart(body));
    }

    /**
     * 完成账号激活（设置初始密码等）
    */
    @PostMapping("/auth/activation/complete")
    public ApiResponse<?> activationComplete(@RequestBody ActivationCompleteDTO body) {
        service.activationComplete(body);
        return ApiResponse.ok(null);
    }

    /**
     * 开始找回密码身份验证流程
    */
    @PostMapping("/auth/recovery/verify")
    public ApiResponse<?> recoveryVerify(@RequestBody RecoveryVerifyDTO body) {
        return ApiResponse.ok(service.recoveryVerify(body));
    }

    /**
     * 完成找回密码（重置为新密码）
    */
    @PostMapping("/auth/recovery/complete")
    public ApiResponse<?> recoveryComplete(@RequestBody RecoveryCompleteDTO body) {
        service.recoveryComplete(body);
        return ApiResponse.ok(null);
    }

    /**
     * 修改当前登录用户的密码
    */
    @PutMapping("/auth/password")
    public ApiResponse<?> changePassword(@RequestBody ChangePasswordDTO body) {
        service.changePassword(body);
        return ApiResponse.ok(null);
    }

    /**
     * 绑定或修改当前登录用户的手机号
    */
    @PutMapping("/auth/contacts/phone")
    public ApiResponse<?> phoneContact(@RequestBody ChangeContactDTO body) {
        service.changeContact("phone", body);
        return ApiResponse.ok(null);
    }

    /**
     * 绑定或修改当前登录用户的电子邮箱
    */
    @PutMapping("/auth/contacts/email")
    public ApiResponse<?> emailContact(@RequestBody ChangeContactDTO body) {
        service.changeContact("email", body);
        return ApiResponse.ok(null);
    }

    /**
     * 用户退出登录，清除 Token 凭证
    */
    @PostMapping("/auth/logout")
    public ApiResponse<?> logout(HttpServletRequest request) {
        service.logout(request.getHeader("Authorization"));
        return ApiResponse.ok(null);
    }

    /**
     * 获取当前登录用户的详细个人资料
    */
    @GetMapping("/users/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok(profileService.loadProfile());
    }

    /**
     * 上传个人头像，系统自动审核，未通过将直接驳回
     */
    @PostMapping("/auth/profile/avatar")
    public ApiResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.ok(profileService.uploadAvatar(file));
    }
}
