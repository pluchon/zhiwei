package com.campus.system.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

// 工单组合查询请求参数
@Data
public class RepairOrderQueryDTO {

    // 工单编号，精确查询
    private String orderNo;

    // 标题关键词，模糊查询
    private String titleKeyword;

    // 创建时间起始
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeFrom;

    // 创建时间结束
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeTo;

    // 完成时间起始，管理员今日完成筛选使用
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTimeFrom;

    // 完成时间结束
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTimeTo;

    // 工单状态
    private Integer status;

    // 故障类型主键
    private Long categoryId;

    // 校区主键
    private Long campusId;

    // 楼栋主键
    private Long buildingId;

    // 报修人账号或姓名关键词，仅管理员可用
    private String reporterKeyword;

    // 当前维修师傅账号或姓名关键词，仅管理员可用
    private String repairerKeyword;

    // 是否筛选长时间未进展，仅管理员可用
    private Boolean longStagnant;

    // 快捷筛选编码
    private String quickFilter;

    // 资产编号，精确查询
    private String assetNo;

    // 资产名称关键词
    private String assetNameKeyword;

    // 是否已导出（0未导出 1已导出），仅管理员可用
    private Integer exportedFlag;

    // 是否疑似重复报修（0否 1是），仅管理员可用
    private Integer suspectedDuplicate;

    // 指定导出的工单主键，逗号分隔；仅导出接口使用
    private String orderIds;
}
