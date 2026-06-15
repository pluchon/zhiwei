package com.campus.system.service.interfaces;

/**
 * 验证码发送服务接口。
 */
public interface VerificationSender {

    void send(String target, String code);
}
