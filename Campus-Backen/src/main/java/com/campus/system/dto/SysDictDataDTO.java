package com.campus.system.dto;

import lombok.Data;

// 字典数据维护请求参数
@Data
public class SysDictDataDTO {

    // 字典类型编码
    private String dictType;

    // 字典显示文本
    private String dictLabel;

    // 字典业务值
    private String dictValue;

    // 显示顺序
    private Integer sortOrder;

    // 数据状态：0 正常，1 停用
    private Integer status;

    // 字典备注
    private String remark;
}
