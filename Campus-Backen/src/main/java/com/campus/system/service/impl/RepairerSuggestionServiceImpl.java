package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.enums.NotificationType;
import com.campus.system.common.enums.SuggestionCategory;
import com.campus.system.common.enums.SuggestionStatus;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.converter.RepairerSuggestionConverter;
import com.campus.system.dto.RepairerSuggestionHandleDTO;
import com.campus.system.dto.RepairerSuggestionQueryDTO;
import com.campus.system.dto.RepairerSuggestionSubmitDTO;
import com.campus.system.entity.RepairerSuggestion;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.entity.UserNotification;
import com.campus.system.mapper.RepairerSuggestionMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.mapper.UserNotificationMapper;
import com.campus.system.service.interfaces.RepairerSuggestionService;
import com.campus.system.service.interfaces.SsePushService;
import com.campus.system.service.ai.SuggestionSimilarityService;
import com.campus.system.vo.RepairerSuggestionVO;
import com.campus.system.vo.SuggestionSimilarityVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 维修师傅建议业务实现
@Service
public class RepairerSuggestionServiceImpl implements RepairerSuggestionService {

    @Autowired
    private RepairerSuggestionMapper suggestionMapper;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private UserNotificationMapper notifications;

    @Autowired
    private SsePushService ssePushService;

    @Autowired
    private SuggestionSimilarityService suggestionSimilarityService;

    @Override
    public SuggestionSimilarityVO checkSimilarity(RepairerSuggestionSubmitDTO body, Long excludeSuggestionId) {
        requireRepairer();
        return suggestionSimilarityService.detect(SecurityUtils.current().userId(), body, excludeSuggestionId);
    }

    @Override
    public PageResult<RepairerSuggestionVO> myList(int pageNum, int pageSize, RepairerSuggestionQueryDTO query) {
        requireRepairer();
        LambdaQueryWrapper<RepairerSuggestion> wrapper = baseWrapper(query);
        wrapper.eq(RepairerSuggestion::getRepairerId, SecurityUtils.current().userId());
        IPage<RepairerSuggestion> page = suggestionMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return enrichPage(page);
    }

    @Override
    public PageResult<RepairerSuggestionVO> adminList(int pageNum, int pageSize, RepairerSuggestionQueryDTO query) {
        requireAdmin();
        LambdaQueryWrapper<RepairerSuggestion> wrapper = baseWrapper(query);
        wrapper.eq(RepairerSuggestion::getWithdrawnFlag, 0);
        IPage<RepairerSuggestion> page = suggestionMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return enrichPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairerSuggestionVO submit(RepairerSuggestionSubmitDTO body) {
        CurrentUser me = requireRepairer();
        RepairerSuggestion suggestion = new RepairerSuggestion();
        suggestion.setRepairerId(me.userId());
        applyContent(suggestion, body);
        suggestion.setStatus(SuggestionStatus.PENDING.getCode());
        suggestion.setWithdrawnFlag(0);
        suggestionMapper.insert(suggestion);
        notifyAdmins(suggestion);
        return enrichSingle(suggestionMapper.selectById(suggestion.getSuggestionId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairerSuggestionVO update(Long id, RepairerSuggestionSubmitDTO body) {
        RepairerSuggestion suggestion = requireOwnEditable(id);
        applyContent(suggestion, body);
        suggestion.setStatus(SuggestionStatus.PENDING.getCode());
        suggestion.setWithdrawnFlag(0);
        suggestionMapper.updateById(suggestion);
        notifyAdmins(suggestionMapper.selectById(id));
        return enrichSingle(suggestionMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        RepairerSuggestion suggestion = requireOwnEditable(id);
        suggestion.setWithdrawnFlag(1);
        suggestionMapper.updateById(suggestion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, RepairerSuggestionHandleDTO body) {
        requireAdmin();
        RepairerSuggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null || suggestion.getDeleteState() != 0 || suggestion.getWithdrawnFlag() == 1) {
            throw BusinessException.notFound("建议不存在");
        }
        if (!SuggestionStatus.PENDING.getCode().equals(suggestion.getStatus())) {
            throw BusinessException.conflict("仅待处理建议可处理");
        }
        String status = required(body.getStatus(), "status");
        if (!SuggestionStatus.ACCEPTED.getCode().equals(status) && !SuggestionStatus.REJECTED.getCode().equals(status)) {
            throw BusinessException.badRequest("处理结果无效");
        }
        suggestion.setStatus(status);
        suggestion.setAdminReply(required(body.getAdminReply(), "adminReply"));
        suggestion.setHandlerId(SecurityUtils.current().userId());
        suggestion.setHandledTime(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);
        notifyRepairer(suggestion);
    }

    @Override
    public RepairerSuggestionVO detail(Long id) {
        RepairerSuggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null || suggestion.getDeleteState() != 0) {
            throw BusinessException.notFound("建议不存在");
        }
        CurrentUser me = SecurityUtils.current();
        if ("ADMIN".equals(me.roleCode()) || me.userId().equals(suggestion.getRepairerId())) {
            return enrichSingle(suggestion);
        }
        throw BusinessException.forbidden("无权查看该建议");
    }

    private LambdaQueryWrapper<RepairerSuggestion> baseWrapper(RepairerSuggestionQueryDTO query) {
        RepairerSuggestionQueryDTO safe = query == null ? new RepairerSuggestionQueryDTO() : query;
        LambdaQueryWrapper<RepairerSuggestion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RepairerSuggestion::getDeleteState, 0);
        if (safe.getStatus() != null && !safe.getStatus().isBlank()) {
            wrapper.eq(RepairerSuggestion::getStatus, safe.getStatus());
        }
        if (safe.getCategory() != null && !safe.getCategory().isBlank()) {
            wrapper.eq(RepairerSuggestion::getCategory, safe.getCategory());
        }
        wrapper.orderByDesc(RepairerSuggestion::getCreateTime);
        return wrapper;
    }

    private void applyContent(RepairerSuggestion suggestion, RepairerSuggestionSubmitDTO body) {
        suggestion.setCategory(SuggestionCategory.of(required(body.getCategory(), "category")).getCode());
        suggestion.setTitle(required(body.getTitle(), "title"));
        suggestion.setContent(required(body.getContent(), "content"));
    }

    private RepairerSuggestion requireOwnEditable(Long id) {
        requireRepairer();
        RepairerSuggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null || !SecurityUtils.current().userId().equals(suggestion.getRepairerId())) {
            throw BusinessException.forbidden("无权操作该建议");
        }
        if (!SuggestionStatus.PENDING.getCode().equals(suggestion.getStatus()) || suggestion.getWithdrawnFlag() == 1) {
            throw BusinessException.conflict("仅待处理且未撤回的建议可编辑或撤回");
        }
        return suggestion;
    }

    private void notifyAdmins(RepairerSuggestion suggestion) {
        SysRole adminRole = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, "ADMIN"));
        if (adminRole == null) {
            return;
        }
        List<SysUser> admins = users.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRoleId, adminRole.getRoleId()).eq(SysUser::getAccountStatus, 0));
        for (SysUser admin : admins) {
            insertNotification(admin.getUserId(), suggestion.getSuggestionId(), "新的维修师傅建议",
                    "维修师傅提交了新的建议：" + suggestion.getTitle(), NotificationType.SUGGESTION_SUBMITTED);
        }
    }

    private void notifyRepairer(RepairerSuggestion suggestion) {
        String label = SuggestionStatus.of(suggestion.getStatus()).getLabel();
        insertNotification(suggestion.getRepairerId(), suggestion.getSuggestionId(), "建议处理结果",
                "您的建议已被处理，结果：" + label, NotificationType.SUGGESTION_HANDLED);
    }

    private void insertNotification(Long receiverId, Long suggestionId, String title, String content, NotificationType type) {
        UserNotification notification = new UserNotification();
        notification.setReceiverId(receiverId);
        notification.setSuggestionId(suggestionId);
        notification.setNotificationType(type.getCode());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notifications.insert(notification);
        ssePushService.pushNotificationChanged(receiverId);
    }

    private PageResult<RepairerSuggestionVO> enrichPage(IPage<RepairerSuggestion> page) {
        List<RepairerSuggestionVO> vos = page.getRecords().stream().map(this::enrichSingle).toList();
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private RepairerSuggestionVO enrichSingle(RepairerSuggestion suggestion) {
        RepairerSuggestionVO vo = RepairerSuggestionConverter.toVO(suggestion);
        SysUser repairer = users.selectById(suggestion.getRepairerId());
        if (repairer != null) {
            vo.setRepairerUserNo(repairer.getUserNo());
            vo.setRepairerRealName(repairer.getRealName());
            vo.setRepairerAvatar(repairer.getAvatar());
        }
        return vo;
    }

    private CurrentUser requireRepairer() {
        CurrentUser me = SecurityUtils.current();
        if (!"REPAIRER".equals(me.roleCode())) {
            throw BusinessException.forbidden("仅维修师傅可提交建议");
        }
        return me;
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可操作");
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }
}
