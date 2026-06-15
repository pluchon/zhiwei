package com.campus.system.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.service.interfaces.VerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// 验证码发送
@Service
public class VerificationSenderImpl implements VerificationSender {

    private static final Logger log = LoggerFactory.getLogger(VerificationSenderImpl.class);

    @Autowired
    private JavaMailSender mail;

    @Value("${campus.aliyun.access-key-id:}")
    private String keyId;

    @Value("${campus.aliyun.access-key-secret:}")
    private String keySecret;

    @Value("${campus.aliyun.sms-sign-name:}")
    private String sign;

    @Value("${campus.aliyun.sms-template-code:}")
    private String template;

    @Value("${campus.mail.from:}")
    private String from;

    // 对外方法
    @Override
    public void send(String target, String code) {
        if (target.contains("@")) {
            sendMail(target, code);
        } else {
            sendSms(target, code);
        }
    }

    // 电子邮箱
    private void sendMail(String target, String code) {
        if (!StringUtils.hasText(from)) {
            throw BusinessException.conflict("SMTP 邮件服务尚未配置");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(target);
        message.setSubject("校园报修系统验证码");
        message.setText("本次验证码：" + code + "，5 分钟内有效。");
        mail.send(message);
    }

    // 手机短信
    private void sendSms(String target, String code) {
        if (!StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret) || !StringUtils.hasText(sign) || !StringUtils.hasText(template)) {
            throw BusinessException.conflict("阿里云号码认证短信服务尚未配置");
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(keyId)
                    .setAccessKeySecret(keySecret);
            config.endpoint = "dypnsapi.aliyuncs.com";
            Client client = new Client(config);
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(sign)
                    .setTemplateCode(template)
                    .setPhoneNumber(target)
                    .setTemplateParam("{\"code\":\"" + code + "\",\"min\":\"5\"}");
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            if (response.getBody() == null || !"OK".equalsIgnoreCase(response.getBody().getCode())) {
                String message = response.getBody() == null ? "未知错误" : response.getBody().getMessage();
                log.warn("号码认证短信发送失败，target={}，原因={}", maskPhone(target), message);
                throw BusinessException.conflict("短信发送失败：" + message);
            }
            log.info("号码认证短信发送成功，target={}", maskPhone(target));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception e) {
            log.warn("号码认证短信发送异常，target={}，原因={}", maskPhone(target), e.getMessage());
            throw BusinessException.conflict("短信发送失败");
        }
    }

    private String maskPhone(String target) {
        if (target == null || target.length() < 7) {
            return "***";
        }
        return target.substring(0, 3) + "****" + target.substring(target.length() - 4);
    }
}
