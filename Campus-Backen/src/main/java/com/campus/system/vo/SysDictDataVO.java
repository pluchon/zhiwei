package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 字典数据响应数据
@Data
public class SysDictDataVO {

    // 字典数据主键
    private Long dictDataId;

    // 所属字典类型编码
    private String dictType;

    // 字典显示文本
    private String dictLabel;

    // 字典业务值
    private String dictValue;

    // 显示顺序
    private Integer sortOrder;

    // 字典数据状态：0 正常，1 停用
    private Integer status;

    // 字典备注
    private String remark;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
