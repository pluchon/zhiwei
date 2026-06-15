package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 字典类型响应数据
@Data
public class SysDictTypeVO {

    // 字典类型主键
    private Long dictTypeId;

    // 字典类型名称
    private String dictName;

    // 字典类型编码
    private String dictType;

    // 字典类型状态：0 正常，1 停用
    private Integer status;

    // 字典类型备注
    private String remark;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
