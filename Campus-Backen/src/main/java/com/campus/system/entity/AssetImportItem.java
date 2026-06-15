package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 待审核资产卡片实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("asset_import_item")
public class AssetImportItem extends BaseEntity {

    // 资产卡片主键
    @TableId
    private Long itemId;

    // 所属导入批次主键
    private Long batchId;

    // 源文件行号
    @TableField("`row_number`")
    private Integer rowNumber;

    // 识别出的资产名称
    private String assetName;

    // 识别出的分类文本
    private String categoryText;

    // 管理员选择的资产分类主键
    private Long assetCategoryId;

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

    // 识别出的位置文本
    private String locationText;

    // 管理员选择的校区主键
    private Long campusId;

    // 管理员选择的楼栋主键
    private Long buildingId;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置描述
    private String locationDetail;

    // 卡片状态
    private String status;

    // 疑似重复提示
    private String duplicateHint;

    // 确认失败原因
    private String failureReason;

    // 确认入库后关联的正式资产主键
    private Long confirmedAssetId;
}
