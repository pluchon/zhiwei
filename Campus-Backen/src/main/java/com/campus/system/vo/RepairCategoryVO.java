package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 故障类型响应数据
@Data
public class RepairCategoryVO {

    // 故障类型主键
    private Long categoryId;

    // 故障类型名称
    private String categoryName;

    // 故障类型说明
    private String description;

    // 类型状态：0 启用，1 停用
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
