package com.campus.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.system.entity.ManualAccountRecovery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

// 账号人工恢复申请数据访问
@Mapper
public interface ManualAccountRecoveryMapper extends BaseMapper<ManualAccountRecovery> {

    @Update("UPDATE manual_account_recovery SET status=#{toStatus}, reviewer_admin_id=#{reviewerId}, review_note=#{reviewNote}, "
            + "approved_time=#{approvedTime}, expire_time=#{expireTime}, update_time=NOW() "
            + "WHERE recovery_id=#{recoveryId} AND status='PENDING' AND delete_state=0")
    int approveIfPending(@Param("recoveryId") Long recoveryId, @Param("toStatus") String toStatus, @Param("reviewerId") Long reviewerId,
            @Param("reviewNote") String reviewNote, @Param("approvedTime") java.time.LocalDateTime approvedTime,
            @Param("expireTime") java.time.LocalDateTime expireTime);

    @Update("UPDATE manual_account_recovery SET status='REJECTED', reviewer_admin_id=#{reviewerId}, review_note=#{reviewNote}, update_time=NOW() "
            + "WHERE recovery_id=#{recoveryId} AND status='PENDING' AND delete_state=0")
    int rejectIfPending(@Param("recoveryId") Long recoveryId, @Param("reviewerId") Long reviewerId, @Param("reviewNote") String reviewNote);

    @Update("UPDATE manual_account_recovery SET delete_state=1, update_time=NOW() "
            + "WHERE recovery_id=#{recoveryId} AND status='PENDING' AND applicant_admin_id=#{adminId} AND delete_state=0")
    int cancelIfPending(@Param("recoveryId") Long recoveryId, @Param("adminId") Long adminId);

    @Update("UPDATE manual_account_recovery SET status='COMPLETED', completed_time=NOW(), update_time=NOW() "
            + "WHERE recovery_id=#{recoveryId} AND status='APPROVED' AND expire_time > NOW() AND delete_state=0")
    int completeIfApproved(@Param("recoveryId") Long recoveryId);

    @Update("UPDATE manual_account_recovery SET status='EXPIRED', update_time=NOW() "
            + "WHERE recovery_id=#{recoveryId} AND status='APPROVED' AND expire_time <= NOW() AND delete_state=0")
    int expireIfApproved(@Param("recoveryId") Long recoveryId);
}
