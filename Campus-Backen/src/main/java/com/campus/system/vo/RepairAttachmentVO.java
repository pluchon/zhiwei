package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 工单附件响应数据
@Data
public class RepairAttachmentVO {

    // 附件主键
    private Long attachmentId;

    // 所属工单主键
    private Long orderId;

    // 所属维修记录主键，为空表示现场图
    private Long recordId;

    // OSS 私有对象键
    private String objectKey;

    // OSS 预签名临时访问 URL
    private String signedUrl;

    // 上传人用户主键
    private Long uploaderId;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
