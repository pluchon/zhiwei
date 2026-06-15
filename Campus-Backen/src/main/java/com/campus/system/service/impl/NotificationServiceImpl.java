package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.converter.EntityVOConverter;
import com.campus.system.dto.NotificationBatchReadDTO;
import com.campus.system.entity.UserNotification;
import com.campus.system.mapper.UserNotificationMapper;
import com.campus.system.service.interfaces.NotificationService;
import com.campus.system.vo.UnreadStateVO;
import com.campus.system.vo.UserNotificationVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 站内通知业务实现。
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private UserNotificationMapper mapper;

    @Override
    public PageResult<UserNotificationVO> list(int pageNum, int pageSize, Integer isRead, Integer notificationType) {
        IPage<UserNotification> page = mapper.selectPage(Page.of(pageNum, pageSize), Wrappers.<UserNotification>lambdaQuery().eq(UserNotification::getReceiverId, SecurityUtils.current().userId())
                        .eq(isRead != null, UserNotification::getIsRead, isRead)
                        .eq(notificationType != null, UserNotification::getNotificationType, notificationType).orderByDesc(UserNotification::getCreateTime));
        return new PageResult<>(EntityVOConverter.toUserNotificationVOList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UnreadStateVO unreadState() {
        long count = mapper.selectCount(Wrappers.<UserNotification>lambdaQuery().eq(UserNotification::getReceiverId, SecurityUtils.current().userId()).eq(UserNotification::getIsRead, 0));
        UnreadStateVO vo = new UnreadStateVO();
        vo.setHasUnread(count > 0);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        UserNotification notification = mapper.selectById(id);
        if (notification == null || !SecurityUtils.current().userId().equals(notification.getReceiverId())) {
            throw BusinessException.forbidden("无权操作");
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            return;
        }
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        mapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadBatch(NotificationBatchReadDTO body) {
        if (body.getNotificationIds() == null || body.getNotificationIds().isEmpty()) {
            throw BusinessException.badRequest("notificationIds 不能为空");
        }
        Long me = SecurityUtils.current().userId();
        List<UserNotification> items = mapper.selectByIds(body.getNotificationIds());
        for (UserNotification notification : items) {
            if (!me.equals(notification.getReceiverId())) {
                throw BusinessException.forbidden("批量请求包含他人通知");
            }
        }
        for (Long id : body.getNotificationIds()) {
            markRead(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long me = SecurityUtils.current().userId();
        mapper.update(null, Wrappers.<UserNotification>lambdaUpdate().eq(UserNotification::getReceiverId, me)
                .eq(UserNotification::getIsRead, 0).set(UserNotification::getIsRead, 1).set(UserNotification::getReadTime, LocalDateTime.now()));
    }
}
