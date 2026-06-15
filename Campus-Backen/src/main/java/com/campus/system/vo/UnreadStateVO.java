package com.campus.system.vo;

import lombok.Data;

// 未读通知状态响应数据
@Data
public class UnreadStateVO {

    // 是否存在未读通知
    private boolean hasUnread;
}
