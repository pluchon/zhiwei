package com.campus.system.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campus.system.common.exception.BusinessException;
import com.campus.system.service.interfaces.AttachmentCleanupService;
import com.campus.system.service.interfaces.ManualAccountRecoveryService;
import com.campus.system.service.interfaces.OssService;
import com.campus.system.service.interfaces.RepairOrderTaskService;
import com.campus.system.service.interfaces.VerificationSender;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

// 真实外部服务与长周期任务验收，必须显式开启，避免普通测试误发消息。
@SpringBootTest
@TestPropertySource(properties = "campus.verification.expose-code=false")
class ExternalAndScheduledAcceptanceTest {

    @Autowired
    private VerificationSender verificationSender;

    @Autowired
    private OssService ossService;

    @Autowired
    private RepairOrderTaskService repairOrderTaskService;

    @Autowired
    private AttachmentCleanupService attachmentCleanupService;

    @Autowired
    private ManualAccountRecoveryService manualAccountRecoveryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${campus.aliyun.sms-sign-name:}")
    private String smsSignName;

    @Value("${campus.aliyun.sms-template-code:}")
    private String smsTemplateCode;

    private void requireEnabled() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("EXTERNAL_ACCEPTANCE_ENABLED")));
    }

    @Test
    void sendsRealMail() {
        requireEnabled();
        String email = System.getenv("EXTERNAL_TEST_EMAIL");
        Assumptions.assumeTrue(email != null && !email.isBlank());
        verificationSender.send(email, "246810");
        System.out.println("EXTERNAL_ACCEPTANCE MAIL_SENT target=" + maskEmail(email));
    }

    @Test
    void attemptsRealSmsOrConfirmsMissingConfiguration() {
        requireEnabled();
        String phone = System.getenv("EXTERNAL_TEST_PHONE");
        Assumptions.assumeTrue(phone != null && !phone.isBlank());
        if (smsSignName.isBlank() || smsTemplateCode.isBlank()) {
            BusinessException error = assertThrows(BusinessException.class, () -> verificationSender.send(phone, "246810"));
            assertTrue(error.getMessage().contains("号码认证短信服务尚未配置"));
            System.out.println("EXTERNAL_ACCEPTANCE SMS_BLOCKED missing_sign_or_template target=" + maskPhone(phone));
            return;
        }
        verificationSender.send(phone, "246810");
        System.out.println("EXTERNAL_ACCEPTANCE SMS_SENT target=" + maskPhone(phone));
    }

    @Test
    void completesRealOssAndAttachmentCleanupRoundTrip() throws Exception {
        requireEnabled();
        byte[] payload = ("campus external acceptance " + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
        String key = ossService.upload("txt", new ByteArrayInputStream(payload));
        Long attachmentId = null;
        try {
            URL signedUrl = ossService.signedUrl(key);
            assertEquals(new String(payload, StandardCharsets.UTF_8),
                    new String(signedUrl.openStream().readAllBytes(), StandardCharsets.UTF_8));
            jdbc.update("""
                    INSERT INTO repair_attachment
                    (order_id, record_id, object_key, uploader_id, oss_delete_status, oss_delete_retry_count,
                     create_time, update_time, delete_state)
                    VALUES (NULL, NULL, ?, 1, 'NONE', 0, DATE_SUB(NOW(), INTERVAL 25 HOUR), NOW(), 0)
                    """, key);
            attachmentId = jdbc.queryForObject("SELECT attachment_id FROM repair_attachment WHERE object_key=?", Long.class, key);
            attachmentCleanupService.processCleanup();
            Map<String, Object> cleaned = jdbc.queryForMap(
                    "SELECT oss_delete_status, delete_state FROM repair_attachment WHERE attachment_id=?", attachmentId);
            assertEquals("SUCCESS", cleaned.get("oss_delete_status"));
            assertEquals(1, ((Number) cleaned.get("delete_state")).intValue());
            assertThrows(Exception.class, () -> signedUrl.openStream().readAllBytes());
            System.out.println("EXTERNAL_ACCEPTANCE OSS_UPLOAD_DOWNLOAD_DELETE_OK key=" + key);
        } finally {
            if (attachmentId != null) {
                jdbc.update("DELETE FROM repair_attachment WHERE attachment_id=?", attachmentId);
            }
            try {
                ossService.delete(key);
            } catch (Exception ignored) {
                // 清理任务成功时对象已经删除。
            }
        }
    }

    @Test
    void processesLongCycleTasksAndRestoresData() {
        requireEnabled();
        Map<String, Object> workCycle = jdbc.queryForMap("SELECT * FROM repair_work_cycle WHERE work_cycle_id=3");
        Map<String, Object> confirmationCycle = jdbc.queryForMap("SELECT * FROM repair_confirmation_cycle WHERE confirmation_cycle_id=3");
        Map<String, Object> completedOrder = jdbc.queryForMap("SELECT * FROM repair_order WHERE order_id=11");
        Map<String, Object> draftOrder = jdbc.queryForMap("SELECT * FROM repair_order WHERE order_id=3");
        long notificationBaseline = maxId("user_notification", "notification_id");
        long orderLogBaseline = maxId("repair_order_log", "log_id");
        long commentBaseline = maxId("repair_order_comment", "comment_id");
        long operationBaseline = maxId("sys_operation_log", "operation_log_id");
        try {
            jdbc.update("""
                    UPDATE repair_work_cycle
                    SET start_time=DATE_SUB(NOW(), INTERVAL 8 DAY), three_day_reminded=0, seven_day_reminded=0
                    WHERE work_cycle_id=3
                    """);
            repairOrderTaskService.processWorkCycleReminders();
            repairOrderTaskService.processWorkCycleReminders();
            Map<String, Object> workResult = jdbc.queryForMap(
                    "SELECT three_day_reminded, seven_day_reminded FROM repair_work_cycle WHERE work_cycle_id=3");
            assertEquals(1, ((Number) workResult.get("three_day_reminded")).intValue());
            assertEquals(1, ((Number) workResult.get("seven_day_reminded")).intValue());
            assertEquals(3, countAfter("user_notification", "notification_id", notificationBaseline, "order_id=9"));

            jdbc.update("""
                    UPDATE repair_order
                    SET status=5, completion_time=NULL, auto_completed_time=NULL
                    WHERE order_id=11
                    """);
            jdbc.update("""
                    UPDATE repair_confirmation_cycle
                    SET start_time=DATE_SUB(NOW(), INTERVAL 31 DAY), end_time=NULL, active_flag=1,
                        three_day_reminded=0, seven_day_reminded=0, twenty_seven_day_reminded=0, auto_completed=0
                    WHERE confirmation_cycle_id=3
                    """);
            repairOrderTaskService.processConfirmationReminders();
            repairOrderTaskService.processConfirmationReminders();
            assertEquals(3, countAfter("user_notification", "notification_id", notificationBaseline, "order_id=11"));
            repairOrderTaskService.processAutoComplete();
            repairOrderTaskService.processAutoComplete();
            assertEquals(7, jdbc.queryForObject("SELECT status FROM repair_order WHERE order_id=11", Integer.class));
            assertEquals(1, countAfter("repair_order_log", "log_id", orderLogBaseline, "order_id=11"));
            assertEquals(1, countAfter("repair_order_comment", "comment_id", commentBaseline, "order_id=11"));
            assertEquals(1, countAfter("sys_operation_log", "operation_log_id", operationBaseline,
                    "operation_type='AUTO_COMPLETE_ORDER' AND target_id=11"));

            jdbc.update("UPDATE repair_order SET update_time=DATE_SUB(NOW(), INTERVAL 31 DAY) WHERE order_id=3");
            repairOrderTaskService.processDraftCleanup();
            repairOrderTaskService.processDraftCleanup();
            assertEquals(1, jdbc.queryForObject("SELECT delete_state FROM repair_order WHERE order_id=3", Integer.class));

            jdbc.update("""
                    INSERT INTO manual_account_recovery
                    (target_user_id, new_phone, status, applicant_admin_id, reviewer_admin_id, identity_check_note,
                     review_note, approved_time, expire_time, create_time, update_time, delete_state)
                    VALUES (2, '19359810949', 'APPROVED', 1, 1, '外部验收测试', '外部验收测试',
                            DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), NOW(), 0)
                    """);
            Long recoveryId = jdbc.queryForObject(
                    "SELECT recovery_id FROM manual_account_recovery WHERE identity_check_note='外部验收测试'", Long.class);
            manualAccountRecoveryService.expireApprovedRecords();
            manualAccountRecoveryService.expireApprovedRecords();
            assertEquals("EXPIRED", jdbc.queryForObject(
                    "SELECT status FROM manual_account_recovery WHERE recovery_id=?", String.class, recoveryId));
            System.out.println("EXTERNAL_ACCEPTANCE SCHEDULED_3_7_27_30_AND_CLEANUP_OK");
        } finally {
            jdbc.update("DELETE FROM manual_account_recovery WHERE identity_check_note='外部验收测试'");
            jdbc.update("DELETE FROM user_notification WHERE notification_id>?", notificationBaseline);
            jdbc.update("DELETE FROM repair_order_log WHERE log_id>?", orderLogBaseline);
            jdbc.update("DELETE FROM repair_order_comment WHERE comment_id>?", commentBaseline);
            jdbc.update("DELETE FROM sys_operation_log WHERE operation_log_id>?", operationBaseline);
            restoreWorkCycle(workCycle);
            restoreConfirmationCycle(confirmationCycle);
            restoreOrder(completedOrder);
            restoreOrder(draftOrder);
        }
    }

    private long maxId(String table, String column) {
        Long value = jdbc.queryForObject("SELECT COALESCE(MAX(" + column + "), 0) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }

    private int countAfter(String table, String idColumn, long baseline, String condition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + ">? AND " + condition, Integer.class, baseline);
        return count == null ? 0 : count;
    }

    private void restoreWorkCycle(Map<String, Object> row) {
        jdbc.update("""
                UPDATE repair_work_cycle
                SET start_time=?, end_time=?, active_flag=?, three_day_reminded=?, seven_day_reminded=?, update_time=?
                WHERE work_cycle_id=?
                """, row.get("start_time"), row.get("end_time"), row.get("active_flag"), row.get("three_day_reminded"),
                row.get("seven_day_reminded"), row.get("update_time"), row.get("work_cycle_id"));
    }

    private void restoreConfirmationCycle(Map<String, Object> row) {
        jdbc.update("""
                UPDATE repair_confirmation_cycle
                SET start_time=?, end_time=?, active_flag=?, three_day_reminded=?, seven_day_reminded=?,
                    twenty_seven_day_reminded=?, auto_completed=?, update_time=?
                WHERE confirmation_cycle_id=?
                """, row.get("start_time"), row.get("end_time"), row.get("active_flag"), row.get("three_day_reminded"),
                row.get("seven_day_reminded"), row.get("twenty_seven_day_reminded"), row.get("auto_completed"),
                row.get("update_time"), row.get("confirmation_cycle_id"));
    }

    private void restoreOrder(Map<String, Object> row) {
        jdbc.update("""
                UPDATE repair_order
                SET status=?, completion_time=?, auto_completed_time=?, version=?, update_time=?, delete_state=?
                WHERE order_id=?
                """, row.get("status"), row.get("completion_time"), row.get("auto_completed_time"), row.get("version"),
                row.get("update_time"), row.get("delete_state"), row.get("order_id"));
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, Math.min(3, at)) + "***" + email.substring(at);
    }
}
