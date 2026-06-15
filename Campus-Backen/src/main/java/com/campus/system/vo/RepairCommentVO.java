package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 工单评论响应数据
@Data
public class RepairCommentVO {

    // 评论主键
    private Long commentId;

    // 所属工单主键
    private Long orderId;

    // 评论作者用户主键，系统评论可为空
    private Long authorId;

    // 评论类型：0 用户评论，1 系统评论
    private Integer commentType;

    // 评论内容
    private String content;

    // 是否置顶：0 否，1 是
    private Integer isPinned;

    // 是否撤回：0 否，1 是
    private Integer isWithdrawn;

    // 撤回时间
    private LocalDateTime withdrawTime;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
