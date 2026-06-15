package com.campus.system.vo;

import lombok.Data;

// 资产卡片确认失败明细响应数据
@Data
public class AssetImportConfirmFailureVO {

    // 资产卡片主键
    private Long itemId;

    // 失败原因
    private String reason;
}
