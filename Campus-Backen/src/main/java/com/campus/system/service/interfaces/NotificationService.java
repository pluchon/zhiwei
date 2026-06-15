package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.NotificationBatchReadDTO;
import com.campus.system.vo.UnreadStateVO;
import com.campus.system.vo.UserNotificationVO;

/**
 * 站内通知业务接口。
 */
public interface NotificationService {

    PageResult<UserNotificationVO> list(int pageNum, int pageSize, Integer isRead, Integer notificationType);

    UnreadStateVO unreadState();

    void markRead(Long id);

    void markReadBatch(NotificationBatchReadDTO body);

    void markAllRead();
}
