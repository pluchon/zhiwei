package com.campus.system.common.enums;

import lombok.Getter;

// 资产状态变更来源
@Getter
public enum AssetChangeSource {

    ORDER_LINK("ORDER_LINK", "关联工单"),
    ORDER_END("ORDER_END", "工单结束"),
    ADMIN_MANUAL("ADMIN_MANUAL", "管理员手动");

    private final String code;
    private final String label;

    AssetChangeSource(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
