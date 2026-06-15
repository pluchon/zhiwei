package com.campus.system.common.enums;

import lombok.Getter;

// 站内通知类型枚举
@Getter
public enum NotificationType {

    //工单状态流转，接单、派单、退回、完成等
    ORDER_STATUS(0),

    //维修周期三天提醒，通知维修师傅
    WORK_CYCLE_3DAY(1),

    //维修周期七天提醒，同时通知管理员关注，长时间未进展
    WORK_CYCLE_7DAY(2),

    //待确认三天提醒，通知报修人
    CONFIRM_3DAY(3),

    //待确认七天提醒，通知报修人
    CONFIRM_7DAY(4),

    //待确认二十七天提醒，通知报修人，距自动完成三天预警
    CONFIRM_27DAY(5),

    //工单三十天自动完成，通知报修人与维修师傅
    AUTO_COMPLETE(6),

    //长时间未进展管理员关注提醒
    LONG_STAGNANT(7),

    //报修人申请自动完成仲裁，通知管理员
    ARBITRATION_REQUEST(8),

    //维修师傅建议相关通知
    SUGGESTION_SUBMITTED(9),

    //建议处理结果通知
    SUGGESTION_HANDLED(10),

    //头像审核驳回通知
    AVATAR_REJECTED(11);

    private final int code;

    // 通过状态码反查枚举
    NotificationType(int code) {
        this.code = code;
    }
}
