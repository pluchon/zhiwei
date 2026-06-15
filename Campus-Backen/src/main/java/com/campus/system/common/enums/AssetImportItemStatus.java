package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 待审核资产卡片状态枚举
@Getter
public enum AssetImportItemStatus {

    PENDING("PENDING", "待审核"),
    CONFIRMED("CONFIRMED", "已确认入库"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String label;

    AssetImportItemStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AssetImportItemStatus of(String code) {
        for (AssetImportItemStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知资产卡片状态");
    }
}
