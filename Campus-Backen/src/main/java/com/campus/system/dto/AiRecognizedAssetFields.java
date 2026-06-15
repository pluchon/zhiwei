package com.campus.system.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// AI 识别出的资产字段载荷
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecognizedAssetFields {

    // 资产名称
    private String assetName;

    // 资产分类文本
    private String categoryText;

    // 购入日期（yyyy-MM-dd）
    private String purchaseDate;

    // 启用日期（yyyy-MM-dd）
    private String enabledDate;

    // 资产说明
    private String assetDescription;
}
