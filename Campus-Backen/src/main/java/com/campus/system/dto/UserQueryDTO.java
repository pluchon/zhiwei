package com.campus.system.dto;

import lombok.Data;

// 后台用户列表查询与导出参数
@Data
public class UserQueryDTO {

    // 姓名关键词（模糊匹配真实姓名）
    private String keyword;

    // 角色主键，逗号分隔
    private String roleIds;

    // 激活状态，逗号分隔：0 未激活，1 已激活
    private String activationStatuses;

    // 账号状态，逗号分隔：0 正常，1 停用
    private String accountStatuses;

    // 指定导出的用户主键，逗号分隔
    private String userIds;
}
