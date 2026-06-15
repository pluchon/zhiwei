package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 维修的状态表示
@Getter
@AllArgsConstructor
public enum RepairStatus {

    DRAFT(0),
    PENDING_DISPATCH(1),
    PENDING_ACCEPT(2),
    ACCEPTED(3),
    PROCESSING(4),
    PENDING_CONFIRM(5),
    PENDING_ARBITRATION(6),
    COMPLETED(7),
    REJECTED(8),
    CLOSED(9);

    private final int code;

    // 通过状态反查枚举
    public static RepairStatus of(int code) {
        for (RepairStatus value : values()){
            if (value.code == code){
                return value;
            }
        }
      throw BusinessException.badRequest("未知工单状态");
    }
}
