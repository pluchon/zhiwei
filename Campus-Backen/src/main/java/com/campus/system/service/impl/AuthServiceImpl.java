package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.JwtService;
import com.campus.system.common.security.payload.LoginSessionPayload;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.security.payload.ActivationPayload;
import com.campus.system.common.security.payload.CaptchaPayload;
import com.campus.system.common.security.payload.RecoveryPayload;
import com.campus.system.common.security.payload.VerificationPayload;
import com.campus.system.converter.EntityVOConverter;
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
import com.campus.system.entity.SysLoginLog;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.SysLoginLogMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.AuthService;
import com.campus.system.service.interfaces.SsePushService;
import com.campus.system.service.interfaces.UserProfileService;
import com.campus.system.service.interfaces.VerificationSender;
import com.campus.system.vo.ActivationStartVO;
import com.campus.system.vo.CaptchaChallengeVO;
import com.campus.system.vo.CaptchaTicketVO;
import com.campus.system.vo.LoginVO;
import com.campus.system.vo.MeVO;
import com.campus.system.vo.RecoveryTicketVO;
import com.campus.system.vo.VerificationCodeVO;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 认证与账号安全
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RedisTemplate<String, Object> redis;

    @Autowired
    private JwtService jwt;

    @Autowired
    private VerificationSender sender;

    @Autowired
    private SysLoginLogMapper loginLogs;

    @Autowired
    private SsePushService ssePushService;

    @Autowired
    private UserProfileService profileService;

    @Value("${campus.verification.expose-code:false}")
    private boolean exposeCode;

    // 生成行为验证码挑战。
    @Override
    public CaptchaChallengeVO captchaChallenge(CaptchaChallengeDTO body) {
        String challengeId = random();
        redis.opsForValue().set("auth:challenge:" + challengeId, new CaptchaPayload(
                required(body.getScene(), "scene"), body.getTarget() == null ? "" : body.getTarget()), Duration.ofMinutes(2));
        return new CaptchaChallengeVO(challengeId, "SLIDER", 120);
    }

    /**
     * 使用行为验证码挑战换取一次性票据。
     * getAndDelete 是并发保护点，同一个 challengeId 只能有一个请求换票成功。
     */
    @Override
    public CaptchaTicketVO captchaTicket(CaptchaTicketDTO body) {
        Object challenge = redis.opsForValue().getAndDelete("auth:challenge:" + body.getChallengeId());
        if (challenge == null) {
            throw BusinessException.badRequest("行为验证码已失效");
        }
        CaptchaPayload payload = (CaptchaPayload) challenge;
        String scene = required(body.getScene(), "scene");
        String target = body.getTarget() == null ? "" : body.getTarget();
        if (!scene.equals(payload.getScene()) || !target.equals(payload.getTarget())) {
            throw BusinessException.badRequest("行为验证码场景不匹配");
        }
        String ticket = random();
        redis.opsForValue().set("auth:captcha-ticket:" + ticket, new CaptchaPayload(scene, target), Duration.ofMinutes(2));
        return new CaptchaTicketVO(ticket, 120);
    }

    /**
     * 发送短信或邮件验证码。
     * 发送冷却使用 Redis setIfAbsent 原子占位，避免并发请求绕过 60 秒限制。
     */
    @Override
    public VerificationCodeVO sendCode(VerificationCodeDTO body) {
        String scene = required(body.getScene(), "scene");
        String target = required(body.getTarget(), "target");
        consumeCaptcha(body.getCaptchaTicket(), scene, target);
        String cooldown = "auth:verification:cooldown:" + scene + ":" + Integer.toHexString(target.hashCode());
        if (Boolean.FALSE.equals(redis.opsForValue().setIfAbsent(cooldown, "1", Duration.ofSeconds(60)))) {
            throw BusinessException.tooMany("请稍后再获取验证码");
        }
        String id = random();
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        redis.opsForValue().set("auth:verification:" + id, new VerificationPayload(scene, target, code, 0), Duration.ofMinutes(5));
        if (!exposeCode) {
            sender.send(target, code);
        }
        VerificationCodeVO result = new VerificationCodeVO();
        result.setVerificationId(id);
        result.setRetryAfter(60);
        if (exposeCode) {
            result.setDevelopmentCode(code);
        }
        return result;
    }

    /**
     * 账号密码登录。
     * 登录失败次数写入 Redis，达到阈值后锁定账号一段时间，防止暴力破解。
     */
    @Override
    public LoginVO loginPassword(PasswordLoginDTO body) {
        String userNo = required(body.getUserNo(), "userNo");
        consumeCaptcha(body.getCaptchaTicket(), "LOGIN_PASSWORD", userNo);
        String lock = "auth:login-lock:account:" + Integer.toHexString(userNo.hashCode());
        if (redis.hasKey(lock)) {
            throw BusinessException.tooMany("密码登录已暂时锁定");
        }
        SysUser user = users.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserNo, userNo));
        if (user == null || !encoder.matches(required(body.getPassword(), "password"), user.getPassword())) {
            Long count = redis.opsForValue().increment("auth:login-failure:account:" + Integer.toHexString(userNo.hashCode()));
            if (count != null && count >= 5) {
                redis.opsForValue().set(lock, "1", Duration.ofMinutes(15));
            }
            loginLog(user, userNo, "PASSWORD", 1, "账号或密码错误");
            throw BusinessException.unauthorized("账号或密码错误");
        }
        ensureUsable(user);
        redis.delete("auth:login-failure:account:" + Integer.toHexString(userNo.hashCode()));
        loginLog(user, userNo, "PASSWORD", 0, "登录成功");
        return createSession(user);
    }

    /**
     * 手机号验证码登录。
     * 验证通过后立即发放 JWT 会话。
     */
    @Override
    public LoginVO loginPhone(PhoneLoginDTO body) {
        VerificationPayload verified = verifyCode(body.getVerificationId(), body.getVerificationCode(), "LOGIN_SMS");
        SysUser user = users.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhoneNumber, verified.getTarget()));
        if (user == null) {
            throw BusinessException.unauthorized("登录失败");
        }
        ensureUsable(user);
        loginLog(user, maskPhone(user.getPhoneNumber()), "SMS", 0, "登录成功");
        return createSession(user);
    }

    /**
     * 获取当前登录用户的详细信息及权限列表。
     */
    @Override
    public MeVO me() {
        return profileService.loadProfile();
    }

    /**
     * 退出登录。
     * JWT 里只保存 sessionId，真正会话在 Redis；退出时删除 Redis 会话即可立即失效。
     */
    @Override
    public void logout(String token) {
        try {
            String sessionId = jwt.parse(token.replace("Bearer ", ""));
            Object raw = redis.opsForValue().get("login_tokens:" + sessionId);
            if (raw instanceof LoginSessionPayload session) {
                ssePushService.disconnectUser(session.getUserId());
            }
            redis.delete("login_tokens:" + sessionId);
        } catch (Exception ignored) {
            // 退出接口保持幂等，非法或过期 token 不再抛出额外错误。
        }
    }

    /**
     * 发起账号激活流程。
     * 校验初始密码，验证通过后发放临时激活票据，后续凭票据设置新密码。
     */
    @Override
    public ActivationStartVO activationStart(ActivationStartDTO body) {
        SysUser user = users.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserNo, required(body.getUserNo(), "userNo")));
        if (user == null || !encoder.matches(required(body.getInitialPassword(), "initialPassword"), user.getPassword())) {
            throw BusinessException.unauthorized("账号或初始密码错误");
        }
        if (user.getActivationStatus() == 1) {
            throw BusinessException.conflict("账号已激活");
        }
        if (user.getAccountStatus() != 0) {
            throw BusinessException.forbidden("账号不可用");
        }
        String ticket = random();
        redis.opsForValue().set("auth:activation-ticket:" + ticket, new ActivationPayload(user.getUserId(), user.getPhoneNumber()), Duration.ofMinutes(10));
        return new ActivationStartVO(ticket, maskPhone(user.getPhoneNumber()));
    }

    /**
     * 完成账号激活。
     * 校验手机验证码和激活票据，通过后保存新密码并标记账号已激活。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activationComplete(ActivationCompleteDTO body) {
        Object raw = redis.opsForValue().get("auth:activation-ticket:" + body.getActivationTicket());
        if (!(raw instanceof ActivationPayload payload)) {
            throw BusinessException.badRequest("激活票据已失效");
        }
        verifyCode(body.getVerificationId(), body.getVerificationCode(), "ACTIVATION");
        SysUser user = users.selectById(payload.getUserId());
        if (encoder.matches(required(body.getNewPassword(), "newPassword"), user.getPassword())) {
            throw BusinessException.badRequest("新密码不能与初始密码相同");
        }
        user.setPassword(encoder.encode(body.getNewPassword()));
        user.setActivationStatus(1);
        user.setSecurityStamp(random());
        users.updateById(user);
        redis.delete("auth:activation-ticket:" + body.getActivationTicket());
    }

    /**
     * 发起找回密码流程（校验联系方式）。
     * 验证码校验通过后，发放找回密码专用票据。
     */
    @Override
    public RecoveryTicketVO recoveryVerify(RecoveryVerifyDTO body) {
        VerificationPayload verified = verifyCode(body.getVerificationId(), body.getVerificationCode(), null);
        String target = verified.getTarget();
        SysUser user = users.selectOne(Wrappers.<SysUser>lambdaQuery().and(q -> q.eq(SysUser::getPhoneNumber, target).or().eq(SysUser::getEmail, target)));
        if (user == null) {
            throw BusinessException.badRequest("验证失败");
        }
        ensureUsable(user);
        String ticket = random();
        redis.opsForValue().set("auth:recovery-ticket:" + ticket, new RecoveryPayload(user.getUserId(), target.contains("@")), Duration.ofMinutes(10));
        return new RecoveryTicketVO(ticket);
    }

    /**
     * 完成密码恢复。
     * 恢复票据使用 getAndDelete 原子消费，避免同一票据被并发请求重复重置密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoveryComplete(RecoveryCompleteDTO body) {
        Object raw = redis.opsForValue().getAndDelete("auth:recovery-ticket:" + body.getRecoveryTicket());
        if (!(raw instanceof RecoveryPayload payload)) {
            throw BusinessException.badRequest("恢复票据已失效");
        }
        SysUser user = users.selectById(payload.getUserId());
        if (encoder.matches(required(body.getNewPassword(), "newPassword"), user.getPassword())) {
            throw BusinessException.badRequest("新密码不能与当前密码相同");
        }
        user.setPassword(encoder.encode(body.getNewPassword()));
        user.setSecurityStamp(random());
        if (payload.isEmail()) {
            user.setPhoneConfirmRequired(1);
        }
        users.updateById(user);
        clearSessions(user.getUserId());
    }

    /**
     * 登录状态下修改密码。
     * 修改成功后自动清理该用户所有在线会话，需要重新登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO body) {
        SysUser user = users.selectById(SecurityUtils.current().userId());
        if (!encoder.matches(required(body.getOldPassword(), "oldPassword"), user.getPassword())) {
            throw BusinessException.badRequest("当前密码错误");
        }
        if (encoder.matches(required(body.getNewPassword(), "newPassword"), user.getPassword())) {
            throw BusinessException.badRequest("新密码不能与当前密码相同");
        }
        user.setPassword(encoder.encode(body.getNewPassword()));
        user.setSecurityStamp(random());
        users.updateById(user);
        clearSessions(user.getUserId());
    }

    /**
     * 换绑手机号或邮箱。
     * 先校验当前身份，再校验新联系方式；换绑后刷新 securityStamp 并清理旧会话。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeContact(String type, ChangeContactDTO body) {
        SysUser user = users.selectById(SecurityUtils.current().userId());
        VerificationPayload identity = verifyCode(body.getIdentityVerificationId(), body.getIdentityCode(), null);
        VerificationPayload next = verifyCode(body.getNewVerificationId(), body.getNewCode(), null);
        String trusted = identity.getTarget();
        if (!trusted.equals(user.getPhoneNumber()) && !trusted.equals(user.getEmail())) {
            throw BusinessException.forbidden("当前身份验证失败");
        }
        String value = next.getTarget();
        if ("phone".equals(type)) {
            ensurePhoneUnused(value, user.getUserId());
            user.setPhoneNumber(value);
            user.setPhoneConfirmRequired(0);
        } else {
            ensureEmailUnused(value, user.getUserId());
            user.setEmail(value);
        }
        user.setSecurityStamp(random());
        users.updateById(user);
        clearSessions(user.getUserId());
    }

    /**
     * 清理指定用户的全部会话。
     * 密码、securityStamp 或联系方式变更后必须执行，确保旧 token 立刻不可用。
     */
    @Override
    public void clearSessions(Long userId) {
        var ids = redis.opsForSet().members("auth:user-sessions:" + userId);
        if (ids != null) {
            ids.forEach(id -> redis.delete("login_tokens:" + id));
        }
        redis.delete("auth:user-sessions:" + userId);
    }

    private VerificationPayload verifyCode(String id, String code, String expected) {
        Object raw = redis.opsForValue().get("auth:verification:" + id);
        if (!(raw instanceof VerificationPayload payload)) {
            throw BusinessException.badRequest("验证码已失效");
        }
        if (expected != null && !expected.equals(payload.getScene())) {
            throw BusinessException.badRequest("验证码场景不匹配");
        }
        if (!required(code, "verificationCode").equals(payload.getCode())) {
            throw BusinessException.badRequest("验证码错误");
        }
        redis.delete("auth:verification:" + id);
        return payload;
    }

    /**
     * 消费行为验证码票据。
     * 票据、场景和目标必须同时匹配，并通过 getAndDelete 保证只能消费一次。
     */
    private void consumeCaptcha(String ticket, String scene, String target) {
        Object raw = redis.opsForValue().getAndDelete("auth:captcha-ticket:" + ticket);
        if (!(raw instanceof CaptchaPayload payload) || !scene.equals(payload.getScene()) || !payload.getTarget().equals(target == null ? "" : target)) {
            throw BusinessException.badRequest("行为验证票据无效");
        }
    }

    private LoginVO createSession(SysUser user) {
        String session = random();
        LoginSessionPayload value = new LoginSessionPayload(user.getUserId(), user.getSecurityStamp());
        redis.opsForValue().set("login_tokens:" + session, value, jwt.ttl());
        redis.opsForSet().add("auth:user-sessions:" + user.getUserId(), session);
        return new LoginVO(jwt.create(session), jwt.ttl().toSeconds());
    }

    private void ensureUsable(SysUser user) {
        if (user.getActivationStatus() != 1) {
            throw new BusinessException(460, "账号尚未激活");
        }
        if (user.getAccountStatus() != 0) {
            throw new BusinessException(461, "账号不可用");
        }
    }

    private void ensurePhoneUnused(String phone, Long userId) {
        if (users.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhoneNumber, phone).ne(SysUser::getUserId, userId)) > 0) {
            throw BusinessException.conflict("手机号已被使用");
        }
    }

    private void ensureEmailUnused(String email, Long userId) {
        if (users.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getEmail, email).ne(SysUser::getUserId, userId)) > 0) {
            throw BusinessException.conflict("邮箱已被使用");
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private String random() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String maskPhone(String phone) {
        return phone == null ? "" : phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    private void loginLog(SysUser user, String identifier, String type, int status, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(user == null ? null : user.getUserId());
        log.setLoginIdentifier(identifier);
        log.setLoginType(type);
        log.setStatus(status);
        log.setMessage(message);
        loginLogs.insert(log);
    }
}
