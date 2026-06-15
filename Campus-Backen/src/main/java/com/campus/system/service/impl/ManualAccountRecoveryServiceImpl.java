package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.enums.ManualRecoveryStatus;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.security.payload.VerificationPayload;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.converter.ManualAccountRecoveryConverter;
import com.campus.system.dto.ManualRecoveryCreateDTO;
import com.campus.system.dto.ManualRecoveryPhoneVerifyDTO;
import com.campus.system.dto.ManualRecoveryQueryDTO;
import com.campus.system.dto.ManualRecoveryReviewDTO;
import com.campus.system.entity.ManualAccountRecovery;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.ManualAccountRecoveryMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.AuthService;
import com.campus.system.service.interfaces.ManualAccountRecoveryService;
import com.campus.system.vo.ManualRecoveryVO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 账号人工恢复业务实现
@Service
public class ManualAccountRecoveryServiceImpl implements ManualAccountRecoveryService {

    private static final String SCENE_MANUAL_RECOVERY = "MANUAL_RECOVERY";

    @Autowired
    private ManualAccountRecoveryMapper recoveries;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private AuthService authService;

    @Autowired
    private RedisTemplate<String, Object> redis;

    @Autowired
    private BusinessClock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManualRecoveryVO create(ManualRecoveryCreateDTO body) {
        Long adminId = requireAdmin();
        Long targetUserId = required(body.getTargetUserId(), "targetUserId");
        String newPhone = normalizePhone(required(body.getNewPhone(), "newPhone"));
        String identityCheckNote = required(body.getIdentityCheckNote(), "identityCheckNote");
        requireRecoverableUser(targetUserId);
        ensurePhoneUnused(newPhone, targetUserId);
        ManualAccountRecovery recovery = new ManualAccountRecovery();
        recovery.setTargetUserId(targetUserId);
        recovery.setNewPhone(newPhone);
        recovery.setStatus(ManualRecoveryStatus.PENDING.getCode());
        recovery.setApplicantAdminId(adminId);
        recovery.setIdentityCheckNote(identityCheckNote.trim());
        recoveries.insert(recovery);
        audit(targetUserId, "MANUAL_RECOVERY_CREATE", "创建人工恢复申请 " + recovery.getRecoveryId());
        return enrichSingle(recovery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long recoveryId) {
        Long adminId = requireAdmin();
        ManualAccountRecovery recovery = requireRecovery(recoveryId);
        if (!ManualRecoveryStatus.PENDING.getCode().equals(recovery.getStatus())) {
            throw BusinessException.conflict("仅待复核申请可撤销");
        }
        if (!adminId.equals(recovery.getApplicantAdminId())) {
            throw BusinessException.forbidden("仅发起管理员可撤销");
        }
        if (recoveries.cancelIfPending(recoveryId, adminId) != 1) {
            throw BusinessException.conflict("申请状态已变化");
        }
        audit(recovery.getTargetUserId(), "MANUAL_RECOVERY_CANCEL", "撤销人工恢复申请 " + recoveryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManualRecoveryVO review(Long recoveryId, ManualRecoveryReviewDTO body) {
        Long reviewerId = requireAdmin();
        ManualAccountRecovery recovery = requireRecovery(recoveryId);
        if (!ManualRecoveryStatus.PENDING.getCode().equals(recovery.getStatus())) {
            throw BusinessException.conflict("仅待复核申请可审批");
        }
        if (reviewerId.equals(recovery.getApplicantAdminId())) {
            throw BusinessException.forbidden("发起管理员不能审批本人创建的申请");
        }
        String reviewNote = required(body.getReviewNote(), "reviewNote");
        Boolean approved = body.getApproved();
        if (approved == null) {
            throw BusinessException.badRequest("approved 不能为空");
        }
        if (approved) {
            LocalDateTime approvedTime = clock.now();
            LocalDateTime expireTime = approvedTime.plusDays(3);
            if (recoveries.approveIfPending(recoveryId, ManualRecoveryStatus.APPROVED.getCode(), reviewerId, reviewNote.trim(), approvedTime, expireTime) != 1) {
                throw BusinessException.conflict("申请状态已变化");
            }
            audit(recovery.getTargetUserId(), "MANUAL_RECOVERY_APPROVE", "审批通过人工恢复申请 " + recoveryId);
        } else {
            if (recoveries.rejectIfPending(recoveryId, reviewerId, reviewNote.trim()) != 1) {
                throw BusinessException.conflict("申请状态已变化");
            }
            audit(recovery.getTargetUserId(), "MANUAL_RECOVERY_REJECT", "驳回人工恢复申请 " + recoveryId);
        }
        return enrichSingle(recoveries.selectById(recoveryId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyPhone(Long recoveryId, ManualRecoveryPhoneVerifyDTO body) {
        ManualAccountRecovery recovery = requireRecovery(recoveryId);
        if (!ManualRecoveryStatus.APPROVED.getCode().equals(recovery.getStatus())) {
            throw BusinessException.conflict("仅已通过且未过期的申请可完成验证");
        }
        if (recovery.getExpireTime() != null && !recovery.getExpireTime().isAfter(clock.now())) {
            throw BusinessException.conflict("申请已过期");
        }
        VerificationPayload verified = verifyCode(body.getVerificationId(), body.getVerificationCode(), SCENE_MANUAL_RECOVERY);
        if (!recovery.getNewPhone().equals(normalizePhone(verified.getTarget()))) {
            throw BusinessException.badRequest("验证码手机号与申请不一致");
        }
        SysUser target = users.selectById(recovery.getTargetUserId());
        if (target == null || target.getDeleteState() != 0) {
            throw BusinessException.notFound("目标用户不存在");
        }
        ensurePhoneUnused(recovery.getNewPhone(), target.getUserId());
        if (recoveries.completeIfApproved(recoveryId) != 1) {
            throw BusinessException.conflict("申请状态已变化或已过期");
        }
        target.setPhoneNumber(recovery.getNewPhone());
        target.setPhoneConfirmRequired(0);
        target.setSecurityStamp(random());
        users.updateById(target);
        audit(target.getUserId(), "MANUAL_RECOVERY_COMPLETE", "完成人工恢复换绑 " + recoveryId);
        authService.clearSessions(target.getUserId());
    }

    @Override
    public PageResult<ManualRecoveryVO> search(int pageNum, int pageSize, ManualRecoveryQueryDTO query) {
        requireAdmin();
        ManualRecoveryQueryDTO safeQuery = query == null ? new ManualRecoveryQueryDTO() : query;
        LambdaQueryWrapper<ManualAccountRecovery> wrapper = Wrappers.<ManualAccountRecovery>lambdaQuery().eq(ManualAccountRecovery::getDeleteState, 0);
        if (safeQuery.getTargetUserId() != null) {
            wrapper.eq(ManualAccountRecovery::getTargetUserId, safeQuery.getTargetUserId());
        }
        if (safeQuery.getStatus() != null && !safeQuery.getStatus().isBlank()) {
            wrapper.eq(ManualAccountRecovery::getStatus, safeQuery.getStatus().trim());
        }
        if (safeQuery.getCreateTimeFrom() != null) {
            wrapper.ge(ManualAccountRecovery::getCreateTime, safeQuery.getCreateTimeFrom());
        }
        if (safeQuery.getCreateTimeTo() != null) {
            wrapper.le(ManualAccountRecovery::getCreateTime, safeQuery.getCreateTimeTo());
        }
        if (Boolean.TRUE.equals(safeQuery.getOnlyPending())) {
            wrapper.eq(ManualAccountRecovery::getStatus, ManualRecoveryStatus.PENDING.getCode());
        }
        wrapper.orderByDesc(ManualAccountRecovery::getCreateTime);
        IPage<ManualAccountRecovery> page = recoveries.selectPage(Page.of(pageNum, pageSize), wrapper);
        return new PageResult<>(enrichList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ManualRecoveryVO detail(Long recoveryId) {
        requireAdmin();
        return enrichSingle(requireRecovery(recoveryId));
    }

    @Override
    public ManualRecoveryVO verifyInfo(Long recoveryId) {
        ManualAccountRecovery recovery = requireRecovery(recoveryId);
        if (!ManualRecoveryStatus.APPROVED.getCode().equals(recovery.getStatus())) {
            throw BusinessException.conflict("仅已通过且未过期的申请可完成验证");
        }
        if (recovery.getExpireTime() != null && !recovery.getExpireTime().isAfter(clock.now())) {
            throw BusinessException.conflict("申请已过期");
        }
        return enrichSingle(recovery);
    }

    @Override
    public void expireApprovedRecords() {
        List<ManualAccountRecovery> expired = recoveries.selectList(Wrappers.<ManualAccountRecovery>lambdaQuery().eq(ManualAccountRecovery::getDeleteState, 0)
                .eq(ManualAccountRecovery::getStatus, ManualRecoveryStatus.APPROVED.getCode()).le(ManualAccountRecovery::getExpireTime, clock.now()));
        for (ManualAccountRecovery recovery : expired) {
            try {
                if (recoveries.expireIfApproved(recovery.getRecoveryId()) == 1) {
                    audit(recovery.getTargetUserId(), "MANUAL_RECOVERY_EXPIRE", "人工恢复申请过期 " + recovery.getRecoveryId(), recovery.getApplicantAdminId());
                }
            } catch (Exception ignored) {
                // 单条失败不影响整批过期任务
            }
        }
    }

    private List<ManualRecoveryVO> enrichList(List<ManualAccountRecovery> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = records.stream().map(ManualAccountRecovery::getTargetUserId).collect(Collectors.toSet());
        records.forEach(r -> {
            userIds.add(r.getApplicantAdminId());
            if (r.getReviewerAdminId() != null) {
                userIds.add(r.getReviewerAdminId());
            }
        });
        Map<Long, SysUser> userMap = users.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));
        return records.stream().map(record -> enrich(record, userMap)).toList();
    }

    private ManualRecoveryVO enrichSingle(ManualAccountRecovery record) {
        Map<Long, SysUser> userMap = new HashMap<>();
        SysUser target = users.selectById(record.getTargetUserId());
        if (target != null) {
            userMap.put(target.getUserId(), target);
        }
        SysUser applicant = users.selectById(record.getApplicantAdminId());
        if (applicant != null) {
            userMap.put(applicant.getUserId(), applicant);
        }
        if (record.getReviewerAdminId() != null) {
            SysUser reviewer = users.selectById(record.getReviewerAdminId());
            if (reviewer != null) {
                userMap.put(reviewer.getUserId(), reviewer);
            }
        }
        return enrich(record, userMap);
    }

    private ManualRecoveryVO enrich(ManualAccountRecovery record, Map<Long, SysUser> userMap) {
        ManualRecoveryVO vo = ManualAccountRecoveryConverter.toVO(record);
        vo.setMaskedNewPhone(maskPhone(record.getNewPhone()));
        SysUser target = userMap.get(record.getTargetUserId());
        if (target != null) {
            vo.setTargetUserNo(target.getUserNo());
            vo.setTargetRealName(target.getRealName());
        }
        SysUser applicant = userMap.get(record.getApplicantAdminId());
        if (applicant != null) {
            vo.setApplicantAdminName(applicant.getRealName());
        }
        if (record.getReviewerAdminId() != null) {
            SysUser reviewer = userMap.get(record.getReviewerAdminId());
            if (reviewer != null) {
                vo.setReviewerAdminName(reviewer.getRealName());
            }
        }
        return vo;
    }

    private void requireRecoverableUser(Long userId) {
        SysUser user = users.selectById(userId);
        if (user == null || user.getDeleteState() != 0) {
            throw BusinessException.notFound("目标用户不存在");
        }
        SysRole role = roles.selectById(user.getRoleId());
        if (role == null || "ADMIN".equals(role.getRoleName())) {
            throw BusinessException.badRequest("管理员账号不适用人工恢复");
        }
        if (!List.of("STUDENT", "TEACHER", "REPAIRER").contains(role.getRoleName())) {
            throw BusinessException.badRequest("目标用户角色不适用");
        }
    }

    private ManualAccountRecovery requireRecovery(Long recoveryId) {
        ManualAccountRecovery recovery = recoveries.selectById(recoveryId);
        if (recovery == null || recovery.getDeleteState() != 0) {
            throw BusinessException.notFound("恢复申请不存在");
        }
        return recovery;
    }

    private VerificationPayload verifyCode(String id, String code, String expectedScene) {
        Object raw = redis.opsForValue().get("auth:verification:" + id);
        if (!(raw instanceof VerificationPayload payload)) {
            throw BusinessException.badRequest("验证码已失效");
        }
        if (expectedScene != null && !expectedScene.equals(payload.getScene())) {
            throw BusinessException.badRequest("验证码场景不匹配");
        }
        if (!required(code, "verificationCode").equals(payload.getCode())) {
            throw BusinessException.badRequest("验证码错误");
        }
        redis.delete("auth:verification:" + id);
        return payload;
    }

    private void ensurePhoneUnused(String phone, Long userId) {
        if (users.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhoneNumber, phone).ne(SysUser::getUserId, userId)) > 0) {
            throw BusinessException.conflict("手机号已被使用");
        }
    }

    private String normalizePhone(String phone) {
        String normalized = phone.trim().replaceAll("[\\s-]", "");
        if (!normalized.matches("^1\\d{10}$")) {
            throw BusinessException.badRequest("手机号格式不正确");
        }
        return normalized;
    }

    private String maskPhone(String phone) {
        return phone == null ? "" : phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    private Long requireAdmin() {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可操作");
        }
        return SecurityUtils.current().userId();
    }

    private void audit(Long targetUserId, String type, String description) {
        audit(targetUserId, type, description, SecurityUtils.current().userId());
    }

    private void audit(Long targetUserId, String type, String description, Long operatorId) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(operatorId);
        log.setOperationType(type);
        log.setTargetType("USER");
        log.setTargetId(targetUserId);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Long required(Long value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private String random() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
