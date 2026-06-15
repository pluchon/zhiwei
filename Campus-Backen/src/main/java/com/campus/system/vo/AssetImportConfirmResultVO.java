package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 资产卡片批量确认结果响应数据
@Data
public class AssetImportConfirmResultVO {

    // 成功确认数量
    private int successCount;

    // 失败确认数量
    private int failureCount;

    // 成功卡片主键列表
    private List<Long> successItemIds;

    // 失败明细
    private List<AssetImportConfirmFailureVO> failures;
}
