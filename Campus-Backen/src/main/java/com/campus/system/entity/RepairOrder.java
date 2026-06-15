package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 报修工单实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_order")
public class RepairOrder extends BaseEntity {

    // 工单主键
    @TableId
    private Long orderId;

    // 对外展示的工单编号
    private String orderNo;

    // 客户端请求幂等标识
    private String requestId;

    // 报修人用户主键
    private Long reporterId;

    // 报修时的角色主键快照
    private Long reporterRoleId;

    // 报修时的用户昵称快照
    private String reporterNickname;

    // 报修时的用户头像快照
    private String reporterAvatar;

    // 工单标题
    private String title;

    // 故障详细描述
    private String description;

    // 故障类型主键
    private Long categoryId;

    // 报修类型（NORMAL普通报修 ASSET资产报修）
    private String repairType;

    // 关联资产主键
    private Long assetId;

    // 资产编号快照
    private String assetNoSnapshot;

    // 资产名称快照
    private String assetNameSnapshot;

    // 资产分类快照
    private String assetCategorySnapshot;

    // 资产位置快照
    private String assetLocationSnapshot;

    // 校区主键
    private Long campusId;

    // 报修时校区名称快照
    private String campus;

    // 报修时校区说明快照
    private String campusDescriptionSnapshot;

    // 楼栋主键
    private Long buildingId;

    // 报修时楼栋名称快照
    private String building;

    // 报修时楼栋说明快照
    private String buildingDescriptionSnapshot;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置说明
    private String locationDetail;

    // 报修联系手机
    private String contactPhone;

    // 工单状态（0草稿、1待派单、2待接单、3已接单、4处理中、5待确认、6待仲裁、7已完成、8已驳回、9已关闭）
    private Integer status;

    // 当前维修师傅用户主键
    private Long currentRepairerId;

    // 报修人反馈未解决次数
    private Integer unresolvedCount;

    // 乐观锁版本号
    private Integer version;

    // 工单真正完成时间
    private java.time.LocalDateTime completionTime;

    // 最近一次自动完成时间
    private java.time.LocalDateTime autoCompletedTime;

    // 是否已导出标记
    private Integer exportedFlag;

    // 首次导出时间
    private java.time.LocalDateTime firstExportTime;

    // 是否疑似重复报修
    private Integer suspectedDuplicate;

    // AI 重复判定理由
    private String duplicateReason;

    // 是否已生成 AI 关联推荐（0否 1是）
    private Integer aiLinkGenerated;
}
