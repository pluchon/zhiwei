package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 系统字典数据实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    // 字典数据主键
    @TableId
    private Long dictDataId;

    // 所属字典类型编码
    private String dictType;

    // 字典选项显示文本
    private String dictLabel;

    // 字典选项业务值
    private String dictValue;

    // 显示顺序
    private Integer sortOrder;

    // 字典数据状态（0正常 1停用）
    private Integer status;

    // 字典数据备注
    private String remark;
}
