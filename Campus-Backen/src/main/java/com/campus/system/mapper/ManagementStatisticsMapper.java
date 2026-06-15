package com.campus.system.mapper;

import com.campus.system.vo.AssetCategoryRepairStatVO;
import com.campus.system.vo.AssetRepairRiskItemVO;
import com.campus.system.vo.RepairEfficiencyStatVO;
import com.campus.system.vo.RepairerWorkStatVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 管理统计聚合查询
@Mapper
public interface ManagementStatisticsMapper {

    @Select("SELECT COUNT(*) AS completedCount, "
            + "AVG(TIMESTAMPDIFF(MINUTE, submit_time, first_accept_time)) AS avgFirstAcceptMinutes, "
            + "AVG(TIMESTAMPDIFF(MINUTE, first_process_time, first_result_time)) AS avgProcessMinutes, "
            + "AVG(TIMESTAMPDIFF(MINUTE, submit_time, completion_time)) AS avgCompletionMinutes, "
            + "SUM(CASE WHEN TIMESTAMPDIFF(DAY, submit_time, completion_time) > 3 THEN 1 ELSE 0 END) AS overThreeDaysCount, "
            + "SUM(CASE WHEN TIMESTAMPDIFF(DAY, submit_time, completion_time) > 7 THEN 1 ELSE 0 END) AS overSevenDaysCount "
            + "FROM ( "
            + "  SELECT o.order_id, "
            + "    (SELECT MIN(l.create_time) FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status IN (2,3) AND l.delete_state=0) AS submit_time, "
            + "    (SELECT MIN(a.create_time) FROM repair_assignment a WHERE a.order_id=o.order_id AND a.delete_state=0) AS first_accept_time, "
            + "    (SELECT MIN(l.create_time) FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status=4 AND l.delete_state=0) AS first_process_time, "
            + "    (SELECT MIN(l.create_time) FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status=5 AND l.delete_state=0) AS first_result_time, "
            + "    o.completion_time "
            + "  FROM repair_order o "
            + "  WHERE o.delete_state=0 AND o.status=7 AND o.completion_time >= #{start} AND o.completion_time < #{end} "
            + ") stats WHERE submit_time IS NOT NULL AND first_accept_time IS NOT NULL")
    RepairEfficiencyStatVO repairEfficiency(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM repair_order o "
            + "WHERE o.delete_state=0 AND o.status IN (1,2,3,4,5,6)")
    Long countUnfinishedSnapshot();

    @Select("SELECT COUNT(*) FROM repair_order o "
            + "WHERE o.delete_state=0 AND o.status != 0 "
            + "AND o.create_time < #{moment} "
            + "AND (o.completion_time IS NULL OR o.completion_time >= #{moment})")
    Long countUnfinishedAt(@Param("moment") LocalDateTime moment);

    @Select("SELECT o.asset_id AS assetId, "
            + "COALESCE(a.asset_no, o.asset_no_snapshot) AS assetNo, "
            + "COALESCE(a.asset_name, o.asset_name_snapshot) AS assetName, "
            + "a.enabled_date AS enabledDate, "
            + "c.category_name AS assetCategoryName, "
            + "a.status AS status, "
            + "COUNT(*) AS repairCount "
            + "FROM repair_order o "
            + "LEFT JOIN asset a ON a.asset_id=o.asset_id AND a.delete_state=0 "
            + "LEFT JOIN asset_category c ON c.asset_category_id=a.asset_category_id AND c.delete_state=0 "
            + "WHERE o.delete_state=0 AND o.asset_id IS NOT NULL AND o.repair_type='ASSET' "
            + "AND EXISTS (SELECT 1 FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status IN (2,3) AND l.create_time >= #{start} AND l.create_time < #{end} AND l.delete_state=0) "
            + "GROUP BY o.asset_id, assetNo, assetName, a.enabled_date, c.category_name, a.status "
            + "ORDER BY repairCount DESC LIMIT #{limit}")
    List<AssetRepairRiskItemVO> topRepairedAssets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);

    @Select("SELECT COALESCE(o.asset_category_snapshot, '未知分类') AS categoryName, COUNT(*) AS repairCount "
            + "FROM repair_order o "
            + "WHERE o.delete_state=0 AND o.repair_type='ASSET' "
            + "AND EXISTS (SELECT 1 FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status IN (2,3) AND l.create_time >= #{start} AND l.create_time < #{end} AND l.delete_state=0) "
            + "GROUP BY categoryName ORDER BY repairCount DESC")
    List<AssetCategoryRepairStatVO> assetCategoryRepairs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT u.user_id AS repairerId, u.user_no AS userNo, u.real_name AS realName, "
            + "u.accepting_state AS acceptingState, u.pause_reason AS pauseReason, u.expected_resume_time AS expectedResumeTime, "
            + "(SELECT COUNT(*) FROM repair_assignment a WHERE a.repairer_id=u.user_id AND a.delete_state=0 "
            + "  AND a.create_time >= #{start} AND a.create_time < #{end}) AS acceptCount, "
            + "(SELECT COUNT(*) FROM repair_order o WHERE o.current_repairer_id=u.user_id AND o.delete_state=0 AND o.status=7 "
            + "  AND o.completion_time >= #{start} AND o.completion_time < #{end}) AS completedCount, "
            + "(SELECT COUNT(*) FROM repair_order o WHERE o.current_repairer_id=u.user_id AND o.delete_state=0 "
            + "  AND o.status IN (3,4)) AS processingCount "
            + "FROM sys_user u "
            + "INNER JOIN sys_role r ON r.role_id=u.role_id AND r.role_name='REPAIRER' "
            + "WHERE u.delete_state=0 AND u.account_status=0 "
            + "ORDER BY u.user_id")
    List<RepairerWorkStatVO> repairerWorkStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT "
            + "(SELECT COUNT(*) FROM repair_assignment a WHERE a.repairer_id=#{repairerId} AND a.delete_state=0 "
            + "  AND a.create_time >= #{start} AND a.create_time < #{end}) AS acceptCount, "
            + "(SELECT COUNT(*) FROM repair_order o WHERE o.current_repairer_id=#{repairerId} AND o.delete_state=0 AND o.status=7 "
            + "  AND o.completion_time >= #{start} AND o.completion_time < #{end}) AS completedCount, "
            + "(SELECT COUNT(*) FROM repair_order o WHERE o.current_repairer_id=#{repairerId} AND o.delete_state=0 "
            + "  AND o.status IN (3,4)) AS processingCount, "
            + "AVG(TIMESTAMPDIFF(MINUTE, first_process_time, completion_time)) AS avgFirstProcessMinutes, "
            + "AVG(TIMESTAMPDIFF(MINUTE, submit_time, completion_time)) AS avgCompletionMinutes "
            + "FROM ( "
            + "  SELECT o.order_id, "
            + "    (SELECT MIN(l.create_time) FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status IN (2,3) AND l.delete_state=0) AS submit_time, "
            + "    (SELECT MIN(l.create_time) FROM repair_order_log l WHERE l.order_id=o.order_id AND l.to_status=4 AND l.delete_state=0) AS first_process_time, "
            + "    o.completion_time "
            + "  FROM repair_order o "
            + "  WHERE o.delete_state=0 AND o.status=7 AND o.current_repairer_id=#{repairerId} "
            + "  AND o.completion_time >= #{start} AND o.completion_time < #{end} "
            + ") stats")
    RepairerWorkStatVO repairerPersonalStats(@Param("repairerId") Long repairerId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
