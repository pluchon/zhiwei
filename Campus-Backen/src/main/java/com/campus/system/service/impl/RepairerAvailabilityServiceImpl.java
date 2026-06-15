package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.enums.RepairerAcceptingState;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.dto.RepairerAvailabilityUpdateDTO;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.RepairerAvailabilityService;
import com.campus.system.vo.RepairerAvailabilityVO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 维修师傅接单状态业务实现
@Service
public class RepairerAvailabilityServiceImpl implements RepairerAvailabilityService {

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Override
    public RepairerAvailabilityVO mine() {
        return toVO(requireCurrentRepairer());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairerAvailabilityVO updateMine(RepairerAvailabilityUpdateDTO body) {
        SysUser repairer = requireCurrentRepairer();
        String state = required(body.getAcceptingState(), "acceptingState");
        RepairerAcceptingState target = RepairerAcceptingState.of(state);
        if (target == RepairerAcceptingState.PAUSED) {
            String reason = required(body.getPauseReason(), "pauseReason");
            repairer.setAcceptingState(RepairerAcceptingState.PAUSED.getCode());
            repairer.setPauseReason(reason.trim());
            repairer.setExpectedResumeTime(body.getExpectedResumeTime());
            users.updateById(repairer);
        } else {
            // 恢复接单时必须显式清空暂停信息，避免 MyBatis-Plus 忽略 null 字段导致历史原因残留
            users.update(null, Wrappers.<SysUser>lambdaUpdate()
                    .set(SysUser::getAcceptingState, RepairerAcceptingState.AVAILABLE.getCode()).set(SysUser::getPauseReason, null)
                    .set(SysUser::getExpectedResumeTime, null).eq(SysUser::getUserId, repairer.getUserId()));
        }
        audit(repairer.getUserId(), "UPDATE_REPAIRER_AVAILABILITY", target == RepairerAcceptingState.PAUSED ? "暂停接单" : "恢复接单");
        return toVO(users.selectById(repairer.getUserId()));
    }

    @Override
    public PageResult<RepairerAvailabilityVO> listForAdmin(int pageNum, int pageSize, String acceptingState) {
        requireAdmin();
        SysRole repairerRole = requireRepairerRole();
        var wrapper = Wrappers.<SysUser>lambdaQuery().eq(SysUser::getRoleId, repairerRole.getRoleId()).eq(SysUser::getDeleteState, 0).orderByDesc(SysUser::getCreateTime);
        if (acceptingState != null && !acceptingState.isBlank()) {
            wrapper.eq(SysUser::getAcceptingState, acceptingState.trim());
        }
        IPage<SysUser> page = users.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<RepairerAvailabilityVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private RepairerAvailabilityVO toVO(SysUser repairer) {
        RepairerAvailabilityVO vo = new RepairerAvailabilityVO();
        vo.setRepairerId(repairer.getUserId());
        vo.setRepairerRealName(repairer.getRealName());
        String state = repairer.getAcceptingState();
        if (state == null || state.isBlank()) {
            state = RepairerAcceptingState.AVAILABLE.getCode();
        }
        vo.setAcceptingState(state);
        vo.setAcceptingStateLabel(RepairerAcceptingState.of(state).getLabel());
        vo.setPauseReason(repairer.getPauseReason());
        vo.setExpectedResumeTime(repairer.getExpectedResumeTime());
        return vo;
    }

    private SysUser requireCurrentRepairer() {
        if (!"REPAIRER".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅维修师傅可操作");
        }
        SysUser repairer = users.selectById(SecurityUtils.current().userId());
        if (repairer == null || repairer.getDeleteState() != 0) {
            throw BusinessException.notFound("用户不存在");
        }
        return repairer;
    }

    private SysRole requireRepairerRole() {
        SysRole role = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, "REPAIRER"));
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        return role;
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可查看");
        }
    }

    private void audit(Long targetId, String type, String description) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(SecurityUtils.current().userId());
        log.setOperationType(type);
        log.setTargetType("USER");
        log.setTargetId(targetId);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }
}
