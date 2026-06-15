package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 资产分类响应数据
@Data
public class AssetCategoryVO {

    // 资产分类主键
    private Long assetCategoryId;

    // 分类名称
    private String categoryName;

    // 分类状态（0启用 1停用）
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
