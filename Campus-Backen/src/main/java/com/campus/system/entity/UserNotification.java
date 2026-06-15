package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 用户站内通知实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("user_notification")
public class UserNotification extends BaseEntity {

    // 通知主键
    @TableId
    private Long notificationId;

    // 通知接收人用户主键
    private Long receiverId;

    // 关联工单主键
    private Long orderId;

    // 关联维修师傅建议主键
    private Long suggestionId;

    // 通知类型
    private Integer notificationType;

    // 通知标题
    private String title;

    // 通知正文
    private String content;

    // 阅读状态（0未读 1已读）
    private Integer isRead;

    // 阅读时间
    private LocalDateTime readTime;
}
