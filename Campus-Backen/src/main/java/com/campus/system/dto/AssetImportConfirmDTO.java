package com.campus.system.dto;

import java.util.List;
import lombok.Data;

// 待审核资产卡片确认入库请求参数
@Data
public class AssetImportConfirmDTO {

    // 导入批次 ID；与 itemIds 二选一，仅传 batchId 时确认该批次全部待审核卡片
    private Long batchId;

    // 待确认卡片主键列表
    private List<Long> itemIds;
}
