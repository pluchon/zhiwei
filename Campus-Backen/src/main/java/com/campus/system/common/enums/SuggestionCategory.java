package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 维修师傅建议分类
@Getter
public enum SuggestionCategory {

    FAULT_TYPE("FAULT_TYPE", "新增或调整故障类型"),
    ASSET_INFO("ASSET_INFO", "资产信息错误或缺失"),
    REPAIR_PROCESS("REPAIR_PROCESS", "报修与维修流程改进"),
    OTHER("OTHER", "其他建议");

    private final String code;
    private final String label;

    SuggestionCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static SuggestionCategory of(String code) {
        for (SuggestionCategory value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知建议分类");
    }
}
