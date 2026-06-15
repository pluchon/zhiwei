package com.campus.system.vo;

import lombok.Data;

// 管理员派单候选维修师傅响应数据
@Data
public class RepairerCandidateVO {

    // 用户主键
    private Long userId;

    // 用户账号
    private String userNo;

    // 用户真实姓名
    private String realName;

    // 师傅繁忙程度编码
    private String busyLevel;

    // 师傅繁忙程度显示名称
    private String busyLevelLabel;

    // 是否具备该类别的维修能力
    private boolean hasCapability;
}
