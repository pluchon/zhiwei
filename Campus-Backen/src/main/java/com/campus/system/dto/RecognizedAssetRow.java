package com.campus.system.dto;

import java.time.LocalDate;
import lombok.Data;

// Excel 识别出的资产行数据
@Data
public class RecognizedAssetRow {

    // 源文件行号
    private int rowNumber;

    // 资产名称
    private String assetName;

    // 分类文本
    private String categoryText;

    // 购入日期
    private LocalDate purchaseDate;

    // 启用日期
    private LocalDate enabledDate;

    // 资产说明
    private String assetDescription;

    // 位置文本
    private String locationText;
}
