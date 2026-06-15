package com.campus.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

// 密码恢复票据响应数据
@Data
@AllArgsConstructor
public class RecoveryTicketVO {

    // 恢复票据
    private String recoveryTicket;
}
