package com.campus.system.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

// 待审核资产卡片响应数据
@Data
public class AssetImportItemVO {

    // 资产卡片主键
    private Long itemId;

    // 所属导入批次主键
    private Long batchId;

    // 源文件行号
    private Integer rowNumber;

    // 资产名称
    private String assetName;

    // 识别出的分类文本
    private String categoryText;

    // 资产分类主键
    private Long assetCategoryId;

    // 资产分类名称
    private String assetCategoryName;

    // 购入日期
    private LocalDate purchaseDate;

    // 启用日期
    private LocalDate enabledDate;

    // 资产说明
    private String assetDescription;

    // 图片导入原图 OSS 对象键
    private String sourceImageObjectKey;

    // AI 识别状态
    private String aiRecognizeStatus;

    // AI 识别状态中文
    private String aiRecognizeStatusLabel;

    // 识别位置文本
    private String locationText;

    // 校区主键
    private Long campusId;

    // 校区名称
    private String campusName;

    // 楼栋主键
    private Long buildingId;

    // 楼栋名称
    private String buildingName;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置描述
    private String locationDetail;

    // 卡片状态
    private String status;

    // 卡片状态中文名称
    private String statusLabel;

    // 疑似重复提示
    private String duplicateHint;

    // 确认失败原因
    private String failureReason;

    // 确认入库后关联的正式资产主键
    private Long confirmedAssetId;

    // 创建时间
    private LocalDateTime createTime;
}
