package com.campus.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

// AI 解析的自然语言工单导出筛选条件
@Data
public class AiNlExportFilterDTO {

    // 创建时间起始
    private LocalDateTime createTimeFrom;

    // 创建时间结束
    private LocalDateTime createTimeTo;

    // 工单状态
    private Integer status;

    // 故障类型主键
    private Long categoryId;

    // 校区主键
    private Long campusId;

    // 楼栋主键
    private Long buildingId;

    // 工单编号
    private String orderNo;

    // 标题关键词
    private String titleKeyword;

    // 资产编号
    private String assetNo;

    // 资产名称关键词
    private String assetNameKeyword;

    // 是否疑似重复
    private Integer suspectedDuplicate;

    // 是否已导出
    private Integer exportedFlag;

    // 是否长时间未进展
    private Boolean longStagnant;
}
