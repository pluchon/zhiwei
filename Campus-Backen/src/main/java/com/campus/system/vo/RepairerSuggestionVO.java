package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 维修师傅建议响应数据
@Data
public class RepairerSuggestionVO {

    // 建议主键
    private Long suggestionId;

    // 提交维修师傅主键
    private Long repairerId;

    // 维修师傅账号
    private String repairerUserNo;

    // 维修师傅姓名
    private String repairerRealName;

    // 维修师傅头像
    private String repairerAvatar;

    // 建议分类
    private String category;

    // 建议分类中文名称
    private String categoryLabel;

    // 建议标题
    private String title;

    // 建议内容
    private String content;

    // 建议状态
    private String status;

    // 建议状态中文名称
    private String statusLabel;

    // 是否已撤回
    private Integer withdrawnFlag;

    // 管理员回复
    private String adminReply;

    // 处理管理员主键
    private Long handlerId;

    // 处理时间
    private LocalDateTime handledTime;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
