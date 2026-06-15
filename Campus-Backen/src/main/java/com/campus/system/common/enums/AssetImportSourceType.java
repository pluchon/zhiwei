package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 资产导入批次来源类型枚举
@Getter
public enum AssetImportSourceType {

    EXCEL("EXCEL", "Excel 导入"),
    IMAGE("IMAGE", "图片导入");

    private final String code;
    private final String label;

    AssetImportSourceType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AssetImportSourceType of(String code) {
        for (AssetImportSourceType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知导入来源类型");
    }
}
