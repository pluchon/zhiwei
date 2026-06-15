package com.campus.system.vo;

import lombok.Data;

// 资产 AI 识别结果响应
@Data
public class AssetImportAiRecognizeResultVO {

    // 资产卡片主键
    private Long itemId;

    // AI 识别状态
    private String aiRecognizeStatus;

    // AI 识别状态中文
    private String aiRecognizeStatusLabel;
}
