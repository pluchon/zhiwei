package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 维修师傅建议实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repairer_suggestion")
public class RepairerSuggestion extends BaseEntity {

    // 建议主键
    @TableId
    private Long suggestionId;

    // 提交维修师傅主键
    private Long repairerId;

    // 建议分类
    private String category;

    // 建议标题
    private String title;

    // 建议内容
    private String content;

    // 建议状态
    private String status;

    // 是否已撤回
    private Integer withdrawnFlag;

    // 管理员回复
    private String adminReply;

    // 处理管理员主键
    private Long handlerId;

    // 处理时间
    private LocalDateTime handledTime;
}
