package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.NotificationType;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.entity.RepairAttachment;
import com.campus.system.entity.RepairComment;
import com.campus.system.entity.RepairConfirmationCycle;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.RepairOrderLog;
import com.campus.system.entity.RepairWorkCycle;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.entity.UserNotification;
import com.campus.system.mapper.RepairAttachmentMapper;
import com.campus.system.mapper.RepairCommentMapper;
import com.campus.system.mapper.RepairConfirmationCycleMapper;
import com.campus.system.mapper.RepairOrderLogMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.mapper.RepairWorkCycleMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.mapper.UserNotificationMapper;
import com.campus.system.service.interfaces.AssetService;
import com.campus.system.service.interfaces.RepairCycleService;
import com.campus.system.service.interfaces.RepairOrderTaskService;
import com.campus.system.service.interfaces.SsePushService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 报修定时任务
@Service
public class RepairOrderTaskServiceImpl implements RepairOrderTaskService {

    @Autowired
    private RepairWorkCycleMapper workCycles;

    @Autowired
    private RepairConfirmationCycleMapper confirmationCycles;

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private UserNotificationMapper notifications;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private RepairCycleService repairCycleService;

    @Autowired
    private RepairOrderLogMapper orderLogs;

    @Autowired
    private RepairCommentMapper comments;

    @Autowired
    private BusinessClock clock;

    @Autowired
    private RepairAttachmentMapper attachments;

    @Autowired
    private AssetService assetService;

    @Autowired
    private SsePushService ssePushService;

    @Override
    public void processWorkCycleReminders() {
        List<RepairWorkCycle> activeCycles = workCycles.selectList(Wrappers.<RepairWorkCycle>lambdaQuery()
                .eq(RepairWorkCycle::getActiveFlag, 1).isNull(RepairWorkCycle::getEndTime));
        for (RepairWorkCycle cycle : activeCycles) {
            try {
                processSingleWorkCycle(cycle);
            } catch (Exception ignored) {
                // 单条失败不中断整批
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void processSingleWorkCycle(RepairWorkCycle cycle) {
        LocalDateTime now = clock.now();
        if (cycle.getThreeDayReminded() == 0 && !cycle.getStartTime().plusDays(3).isAfter(now)) {
            if (claimWorkCycleThreeDayReminder(cycle.getWorkCycleId())) {
                notify(cycle.getRepairerId(), cycle.getOrderId(), "工单处理提醒", "您负责的工单已连续处理满三天，如有进展可及时更新处理说明。", NotificationType.WORK_CYCLE_3DAY);
            }
        }
        cycle = workCycles.selectById(cycle.getWorkCycleId());
        if (cycle != null && cycle.getSevenDayReminded() == 0 && !cycle.getStartTime().plusDays(7).isAfter(now)) {
            if (claimWorkCycleSevenDayReminder(cycle.getWorkCycleId())) {
                  notify(cycle.getRepairerId(), cycle.getOrderId(), "工单处理提醒", "您负责的工单已连续处理满七天，请合理安排处理进度。", NotificationType.WORK_CYCLE_7DAY);
                notifyActiveAdmins(cycle.getOrderId());
            }
        }
    }

    @Override
    public void processConfirmationReminders() {
        List<RepairConfirmationCycle> activeCycles = confirmationCycles.selectList(Wrappers.<RepairConfirmationCycle>lambdaQuery()
                .eq(RepairConfirmationCycle::getActiveFlag, 1).isNull(RepairConfirmationCycle::getEndTime));
        for (RepairConfirmationCycle cycle : activeCycles) {
            try {
                processSingleConfirmationReminder(cycle);
            } catch (Exception ignored) {

            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void processSingleConfirmationReminder(RepairConfirmationCycle cycle) {
        LocalDateTime now = clock.now();
        if (cycle.getThreeDayReminded() == 0 && !cycle.getStartTime().plusDays(3).isAfter(now)) {
            if (claimConfirmationThreeDayReminder(cycle.getConfirmationCycleId())) {
                notify(cycle.getReporterId(), cycle.getOrderId(), "请确认维修结果", "维修结果已提交满三天，如您认可维修结果，可尽快确认完成。", NotificationType.CONFIRM_3DAY);
            }
        }
        cycle = confirmationCycles.selectById(cycle.getConfirmationCycleId());
        if (cycle != null && cycle.getSevenDayReminded() == 0 && !cycle.getStartTime().plusDays(7).isAfter(now)) {
            if (claimConfirmationSevenDayReminder(cycle.getConfirmationCycleId())) {
                notify(cycle.getReporterId(), cycle.getOrderId(), "请确认维修结果", "维修结果已提交满七天，如您认可维修结果，可尽快确认完成。", NotificationType.CONFIRM_7DAY);
            }
        }
        cycle = confirmationCycles.selectById(cycle != null ? cycle.getConfirmationCycleId() : null);
        if (cycle != null && cycle.getTwentySevenDayReminded() == 0 && !cycle.getStartTime().plusDays(27).isAfter(now)) {
            if (claimConfirmationTwentySevenDayReminder(cycle.getConfirmationCycleId())) {
                notify(cycle.getReporterId(), cycle.getOrderId(), "请确认维修结果", "维修结果已提交满二十七天，如仍未操作，满三十天后系统将自动完成该工单。", NotificationType.CONFIRM_27DAY);
            }
        }
    }

    @Override
    public void processAutoComplete() {
          List<RepairConfirmationCycle> activeCycles = confirmationCycles.selectList(Wrappers.<RepairConfirmationCycle>lambdaQuery().eq(RepairConfirmationCycle::getActiveFlag, 1).isNull(RepairConfirmationCycle::getEndTime));
          for (RepairConfirmationCycle cycle : activeCycles) {
              try {
                  processSingleAutoComplete(cycle);
              } catch (Exception ignored) {

              }
          }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void processSingleAutoComplete(RepairConfirmationCycle cycle) {
        if (!clock.now().isBefore(cycle.getStartTime().plusDays(30))) {
            if (!claimConfirmationAutoComplete(cycle.getConfirmationCycleId())) {
                return;
            }
            RepairOrder order = orders.selectById(cycle.getOrderId());
            if (order == null || order.getStatus() != RepairStatus.PENDING_CONFIRM.getCode()) {
                return;
            }
            if (orders.autoComplete(order.getOrderId()) != 1) {
                return;
            }
            repairCycleService.endConfirmationCycle(order.getOrderId());
            RepairOrderLog logEntry = new RepairOrderLog();
            logEntry.setOrderId(order.getOrderId());
            logEntry.setAction(RepairStatus.COMPLETED.getCode());
            logEntry.setFromStatus(RepairStatus.PENDING_CONFIRM.getCode());
            logEntry.setToStatus(RepairStatus.COMPLETED.getCode());
            logEntry.setRemark("系统自动完成待确认工单");
            orderLogs.insert(logEntry);
            RepairComment comment = new RepairComment();
            comment.setOrderId(order.getOrderId());
            comment.setCommentType(1);
            comment.setContent("系统已自动完成该工单。待确认满三十天且报修人未操作。");
            comment.setIsPinned(1);
            comment.setIsWithdrawn(0);
            comments.insert(comment);
            RepairOrder completed = orders.selectById(order.getOrderId());
            assetService.onOrderEnded(completed);
            notify(order.getReporterId(), order.getOrderId(), "工单已自动完成", "该工单待确认已满三十天，系统已自动完成。如对结果有异议，可在七天内申请管理员仲裁。", NotificationType.AUTO_COMPLETE);
            SysOperationLog log = new SysOperationLog();
            log.setOperationType("AUTO_COMPLETE_ORDER");
            log.setTargetType("ORDER");
            log.setTargetId(order.getOrderId());
            log.setDescription("系统自动完成待确认工单");
            operationLogs.insert(log);
        }
    }

    @Override
    public void processDraftCleanup() {
        LocalDateTime threshold = clock.now().minusDays(30);
        List<RepairOrder> drafts = orders.selectList(Wrappers.<RepairOrder>lambdaQuery()
                .eq(RepairOrder::getStatus, RepairStatus.DRAFT.getCode()).eq(RepairOrder::getDeleteState, 0).le(RepairOrder::getUpdateTime, threshold));
        for (RepairOrder draft : drafts) {
            try {
                cleanupSingleDraft(draft.getOrderId());
            } catch (Exception ignored) {

            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupSingleDraft(Long orderId) {
        if (logicDeleteDraft(orderId)) {
            markDraftAttachmentsForCleanup(orderId);
            SysOperationLog log = new SysOperationLog();
            log.setOperationType("AUTO_CLEANUP_DRAFT");
            log.setTargetType("ORDER");
            log.setTargetId(orderId);
            log.setDescription("系统自动逻辑删除超期草稿工单，orderId=" + orderId);
            operationLogs.insert(log);
        }
    }

    // 定时任务，用是否被更新过来判断是否发送通知
    private boolean claimWorkCycleThreeDayReminder(Long workCycleId) {
        return workCycles.update(null, Wrappers.<RepairWorkCycle>lambdaUpdate()
              .set(RepairWorkCycle::getThreeDayReminded, 1).eq(RepairWorkCycle::getWorkCycleId, workCycleId)
                .eq(RepairWorkCycle::getActiveFlag, 1).eq(RepairWorkCycle::getThreeDayReminded, 0)) == 1;
    }

    private boolean claimWorkCycleSevenDayReminder(Long workCycleId) {
        return workCycles.update(null, Wrappers.<RepairWorkCycle>lambdaUpdate()
                  .set(RepairWorkCycle::getSevenDayReminded, 1).eq(RepairWorkCycle::getWorkCycleId, workCycleId)
                      .eq(RepairWorkCycle::getActiveFlag, 1).eq(RepairWorkCycle::getSevenDayReminded, 0)) == 1;
    }

    private boolean claimConfirmationThreeDayReminder(Long confirmationCycleId) {
        return confirmationCycles.update(null, Wrappers.<RepairConfirmationCycle>lambdaUpdate()
                  .set(RepairConfirmationCycle::getThreeDayReminded, 1).eq(RepairConfirmationCycle::getConfirmationCycleId, confirmationCycleId)
                  .eq(RepairConfirmationCycle::getActiveFlag, 1).eq(RepairConfirmationCycle::getThreeDayReminded, 0)) == 1;
    }

    private boolean claimConfirmationSevenDayReminder(Long confirmationCycleId) {
        return confirmationCycles.update(null, Wrappers.<RepairConfirmationCycle>lambdaUpdate()
                  .set(RepairConfirmationCycle::getSevenDayReminded, 1).eq(RepairConfirmationCycle::getConfirmationCycleId, confirmationCycleId)
                  .eq(RepairConfirmationCycle::getActiveFlag, 1).eq(RepairConfirmationCycle::getSevenDayReminded, 0)) == 1;
    }

    private boolean claimConfirmationTwentySevenDayReminder(Long confirmationCycleId) {
        return confirmationCycles.update(null, Wrappers.<RepairConfirmationCycle>lambdaUpdate()
                  .set(RepairConfirmationCycle::getTwentySevenDayReminded, 1).eq(RepairConfirmationCycle::getConfirmationCycleId, confirmationCycleId)
                  .eq(RepairConfirmationCycle::getActiveFlag, 1).eq(RepairConfirmationCycle::getTwentySevenDayReminded, 0)) == 1;
    }

    private boolean claimConfirmationAutoComplete(Long confirmationCycleId) {
        return confirmationCycles.update(null, Wrappers.<RepairConfirmationCycle>lambdaUpdate()
                  .set(RepairConfirmationCycle::getAutoCompleted, 1).set(RepairConfirmationCycle::getEndTime, clock.now())
                  .set(RepairConfirmationCycle::getActiveFlag, null).eq(RepairConfirmationCycle::getConfirmationCycleId, confirmationCycleId)
                  .eq(RepairConfirmationCycle::getActiveFlag, 1).eq(RepairConfirmationCycle::getAutoCompleted, 0)) == 1;
    }

    private boolean logicDeleteDraft(Long orderId) {
        return orders.update(null, Wrappers.<RepairOrder>lambdaUpdate().set(RepairOrder::getDeleteState, 1).eq(RepairOrder::getOrderId, orderId)
                  .eq(RepairOrder::getStatus, RepairStatus.DRAFT.getCode()).eq(RepairOrder::getDeleteState, 0)) == 1;
    }

    private void notifyActiveAdmins(Long orderId) {
        SysRole adminRole = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, "ADMIN"));
        if (adminRole == null) {
            return;
        }
        List<SysUser> admins = users.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRoleId, adminRole.getRoleId()).eq(SysUser::getAccountStatus, 0));
        for (SysUser admin : admins) {
            notify(admin.getUserId(), orderId, "工单长时间未进展", "有工单在当前维修周期内已满七天未结束，请关注处理进展。", NotificationType.LONG_STAGNANT);
        }
    }

    // 向指定用户发送站内通知
    private void notify(Long receiver, Long orderId, String title, String content, NotificationType type) {
        if (receiver == null) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setReceiverId(receiver);
        notification.setOrderId(orderId);
        notification.setNotificationType(type.getCode());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notifications.insert(notification);
        ssePushService.pushNotificationChanged(receiver);
    }

    private void markDraftAttachmentsForCleanup(Long orderId) {
        LocalDateTime now = clock.now();
        attachments.update(null, Wrappers.<RepairAttachment>lambdaUpdate().set(RepairAttachment::getDraftDeletedTime, now)
                .set(RepairAttachment::getCleanupDueTime, now.plusDays(7)).eq(RepairAttachment::getOrderId, orderId).eq(RepairAttachment::getDeleteState, 0));
    }
}
