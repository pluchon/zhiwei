package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 工单响应数据
@Data
public class RepairOrderVO {

    // 工单主键
    private Long orderId;

    // 对外展示的工单编号
    private String orderNo;

    // 客户端幂等请求编号
    private String requestId;

    // 报修人用户主键
    private Long reporterId;

    // 报修时的角色主键快照
    private Long reporterRoleId;

    // 报修人昵称快照
    private String reporterNickname;

    // 报修人头像快照
    private String reporterAvatar;

    // 工单标题
    private String title;

    // 故障描述
    private String description;

    // 故障类型主键
    private Long categoryId;

    // 报修类型
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

    // 校区名称快照
    private String campus;

    // 校区说明快照
    private String campusDescriptionSnapshot;

    // 楼栋主键
    private Long buildingId;

    // 楼栋名称快照
    private String building;

    // 楼栋说明快照
    private String buildingDescriptionSnapshot;

    // 所在楼层
    private String floor;

    // 教室或房间
    private String room;

    // 具体位置说明
    private String locationDetail;

    // 报修联系电话
    private String contactPhone;

    // 工单状态码
    private Integer status;

    // 当前维修师傅用户主键
    private Long currentRepairerId;

    // 报修人反馈未解决次数
    private Integer unresolvedCount;

    // 乐观锁版本号
    private Integer version;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;

    // 工单真正完成时间
    private LocalDateTime completionTime;

    // 自动完成时间
    private LocalDateTime autoCompletedTime;

    // 报修人账号，管理员列表展示
    private String reporterUserNo;

    // 报修人姓名，管理员列表展示
    private String reporterRealName;

    // 当前维修师傅账号，管理员列表展示
    private String repairerUserNo;

    // 当前维修师傅姓名，管理员列表展示
    private String repairerRealName;

    // 是否长时间未进展
    private Boolean longStagnant;

    // 当前维修师傅繁忙程度编码
    private String repairerBusyLevel;

    // 当前维修师傅繁忙程度中文名称
    private String repairerBusyLevelLabel;

    // 是否已导出
    private Integer exportedFlag;

    // 首次导出时间
    private LocalDateTime firstExportTime;

    // 是否疑似重复报修
    private Integer suspectedDuplicate;

    // AI 重复判定理由
    private String duplicateReason;
}
