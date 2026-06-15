package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 站内通知响应数据
@Data
public class UserNotificationVO {

    // 通知主键
    private Long notificationId;

    // 通知接收人用户主键
    private Long receiverId;

    // 关联工单主键
    private Long orderId;

    // 关联维修师傅建议主键
    private Long suggestionId;

    // 通知类型编码
    private Integer notificationType;

    // 通知标题
    private String title;

    // 通知正文
    private String content;

    // 是否已读：0 未读，1 已读
    private Integer isRead;

    // 阅读时间
    private LocalDateTime readTime;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
