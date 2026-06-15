package com.campus.system.dto;

import lombok.Data;

// 管理员处理维修师傅建议请求参数
@Data
public class RepairerSuggestionHandleDTO {

    // 处理结果（ACCEPTED已采纳 REJECTED未采纳）
    private String status;

    // 管理员回复
    private String adminReply;
}
