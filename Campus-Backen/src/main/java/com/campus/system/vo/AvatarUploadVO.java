package com.campus.system.vo;

import lombok.Data;

// 头像上传审核结果
@Data
public class AvatarUploadVO {

    // 审核通过后的可访问头像地址
    private String avatarUrl;

    // 提示信息
    private String message;
}
