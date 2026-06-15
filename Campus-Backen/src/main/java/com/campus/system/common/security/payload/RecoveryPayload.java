package com.campus.system.common.security.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 密码恢复票据载荷，记录恢复用户和身份验证方式，用于完成密码重置后决定是否要求重新确认手机号。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryPayload {

    // 执行密码恢复的用户主键。
    private Long userId;

    // 是否通过邮箱完成身份验证；邮箱恢复后需要提示用户重新确认主手机号。
    private boolean email;
}
