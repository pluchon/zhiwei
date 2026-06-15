package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.enums.BusyLevel;
import com.campus.system.common.enums.NotificationType;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.enums.RepairerAcceptingState;
import com.campus.system.common.enums.RepairType;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.converter.EntityVOConverter;
import com.campus.system.dto.AdminDispatchDTO;
import com.campus.system.dto.AutoCompleteArbitrationDTO;
import com.campus.system.dto.CommentDTO;
import com.campus.system.dto.EvaluationDTO;
import com.campus.system.dto.FollowUpDTO;
import com.campus.system.dto.RepairOrderEditDTO;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.dto.RepairResultDTO;
import com.campus.system.dto.RepairReturnDTO;
import com.campus.system.dto.VersionDTO;
import com.campus.system.entity.RepairAssignment;
import com.campus.system.entity.RepairAttachment;
import com.campus.system.entity.RepairCapability;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.RepairComment;
import com.campus.system.entity.RepairEvaluation;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.RepairOrderLog;
import com.campus.system.entity.RepairRecord;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.entity.UserNotification;
import com.campus.system.mapper.RepairAssignmentMapper;
import com.campus.system.mapper.RepairAttachmentMapper;
import com.campus.system.mapper.RepairCapabilityMapper;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.RepairCommentMapper;
import com.campus.system.mapper.RepairEvaluationMapper;
import com.campus.system.mapper.RepairOrderLogMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.mapper.RepairRecordMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.mapper.UserNotificationMapper;
import com.campus.system.entity.LocationSnapshot;
import com.campus.system.service.interfaces.AssetService;
import com.campus.system.service.interfaces.LocationService;
import com.campus.system.service.interfaces.OssService;
import com.campus.system.service.interfaces.RepairCycleService;
import com.campus.system.service.interfaces.RepairOrderService;
import com.campus.system.service.interfaces.SsePushService;
import com.campus.system.service.repair.DuplicateRepairDetectionService;
import com.campus.system.vo.RepairAttachmentVO;
import com.campus.system.vo.RepairCategoryVO;
import com.campus.system.vo.RepairCommentVO;
import com.campus.system.vo.RepairEvaluationVO;
import com.campus.system.vo.RepairOrderDetailVO;
import com.campus.system.vo.RepairOrderLogVO;
import com.campus.system.vo.RepairOrderSubmitResultVO;
import com.campus.system.vo.RepairOrderVO;
import com.campus.system.vo.DuplicateRepairCheckVO;
import com.campus.system.vo.RepairerCandidateVO;
import com.campus.system.vo.WorkforceSummaryVO;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// 报修工单
@Service
public class RepairOrderServiceImpl implements RepairOrderService {

    @Autowired
    private RepairOrderMapper orders;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private RepairCapabilityMapper capabilities;

    @Autowired
    private RepairAssignmentMapper assignments;

    @Autowired
    private RepairRecordMapper records;

    @Autowired
    private RepairAttachmentMapper attachments;

    @Autowired
    private RepairCommentMapper comments;

    @Autowired
    private RepairOrderLogMapper logs;

    @Autowired
    private RepairEvaluationMapper evaluations;

    @Autowired
    private UserNotificationMapper notifications;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private OssService oss;

    @Autowired
    private LocationService locationService;

    @Autowired
    private RepairCycleService repairCycleService;

    @Autowired
    private DuplicateRepairDetectionService duplicateRepairDetection;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private AssetService assetService;

    @Autowired
    private SsePushService ssePushService;

    /**
     * 获取当前系统已启用的维修类目列表
     */
    @Override
    public List<RepairCategoryVO> enabledCategories() {
        return EntityVOConverter.toRepairCategoryVOList(categories.selectList(Wrappers.<RepairCategory>lambdaQuery().eq(RepairCategory::getStatus, 0)));
    }

    /**
     * 组合条件分页查询工单，按当前角色限定可见范围并支持快捷筛选。
     */
    @Override
    public PageResult<RepairOrderVO> search(int page, int size, RepairOrderQueryDTO query) {
        CurrentUser me = SecurityUtils.current();
        boolean admin = "ADMIN".equals(me.roleCode());
        if (!admin && hasAdminOnlyFilter(query)) {
            throw BusinessException.forbidden("仅管理员可使用该筛选条件");
        }
        RepairOrderQueryDTO safeQuery = query == null ? new RepairOrderQueryDTO() : query;
        LambdaQueryWrapper<RepairOrder> wrapper = buildSearchWrapper(me, admin, safeQuery);
        IPage<RepairOrder> result = orders.selectPage(Page.of(page, size), wrapper);
        return toEnrichedOrderPage(result, me, admin);
    }

    @Override
    public List<RepairOrderVO> listForExportByIds(List<Long> orderIds) {
        CurrentUser me = SecurityUtils.current();
        if (!"ADMIN".equals(me.roleCode())) {
            throw BusinessException.forbidden("仅管理员可导出工单");
        }
        if (orderIds == null || orderIds.isEmpty()) {
            throw BusinessException.badRequest("请先选择要导出的工单");
        }
        if (orderIds.size() > 1000) {
            throw BusinessException.badRequest("单次最多导出 1000 条");
        }
        List<RepairOrder> list = orders.selectList(Wrappers.<RepairOrder>lambdaQuery().in(RepairOrder::getOrderId, orderIds).eq(RepairOrder::getDeleteState, 0));
        if (list.size() != orderIds.size()) {
            throw BusinessException.badRequest("部分工单不存在或无权导出");
        }
        Map<Long, RepairOrder> orderMap = list.stream()
                .collect(Collectors.toMap(RepairOrder::getOrderId, order -> order, (left, right) -> left));
        List<RepairOrder> ordered = orderIds.stream().map(orderMap::get).filter(Objects::nonNull).toList();
        List<RepairOrderVO> records = ordered.stream().map(EntityVOConverter::toRepairOrderVO).collect(Collectors.toList());
        enrichAdminListFields(records);
        for (RepairOrderVO vo : records) {
            enrichOrderVo(vo, me, true);
        }
        return records;
    }

    // 抢单大厅
    @Override
    public PageResult<RepairOrderVO> available(int page, int size, Long campusId, Long buildingId, Long categoryId, String titleKeyword) {
        CurrentUser me = SecurityUtils.current();
        requireAvailableRepairer(me.userId());
        List<Long> myCategories = capabilities.selectList(Wrappers.<RepairCapability>lambdaQuery().eq(RepairCapability::getRepairerId, me.userId()))
                .stream().map(RepairCapability::getCategoryId).toList();
        if (myCategories.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        // 若前端指定了故障类型，取交集（不在能力范围内直接返回空）
        List<Long> effectiveCategories = categoryId != null ? myCategories.stream().filter(categoryId::equals).toList() : myCategories;
        if (effectiveCategories.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<RepairOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RepairOrder::getDeleteState, 0);
        wrapper.eq(RepairOrder::getStatus, RepairStatus.PENDING_ACCEPT.getCode());
        wrapper.in(RepairOrder::getCategoryId, effectiveCategories);
        if (campusId != null) {
            wrapper.eq(RepairOrder::getCampusId, campusId);
        }
        if (buildingId != null) {
            wrapper.eq(RepairOrder::getBuildingId, buildingId);
        }
        if (titleKeyword != null && !titleKeyword.isBlank()) {
            wrapper.like(RepairOrder::getTitle, titleKeyword.trim());
        }
        wrapper.orderByDesc(RepairOrder::getCreateTime);
        IPage<RepairOrder> result = orders.selectPage(Page.of(page, size), wrapper);
        return toEnrichedOrderPage(result, me, false);
    }

    // 获取工单详细信息
    @Override
    public RepairOrderDetailVO detail(Long id) {
        RepairOrder order = requireVisible(id);
        CurrentUser me = SecurityUtils.current();
        boolean admin = "ADMIN".equals(me.roleCode());
        RepairOrderVO orderVo = EntityVOConverter.toRepairOrderVO(order);
        enrichOrderVo(orderVo, me, admin);
        List<RepairAttachmentVO> attachmentVos = EntityVOConverter.toRepairAttachmentVOList(
                attachments.selectList(Wrappers.<RepairAttachment>lambdaQuery().eq(RepairAttachment::getOrderId, id)));fillSignedUrls(attachmentVos);
        List<RepairOrderLogVO> logVos = EntityVOConverter.toRepairOrderLogVOList(logs.selectList(
                        Wrappers.<RepairOrderLog>lambdaQuery().eq(RepairOrderLog::getOrderId, id).orderByAsc(RepairOrderLog::getCreateTime)));
        RepairEvaluation evaluationEntity = evaluations.selectOne(Wrappers.<RepairEvaluation>lambdaQuery()
                .eq(RepairEvaluation::getOrderId, id)
                .eq(RepairEvaluation::getDeleteState, 0));
        RepairEvaluationVO evaluationVo = evaluationEntity == null
                ? null
                : EntityVOConverter.toRepairEvaluationVO(evaluationEntity);
        return new RepairOrderDetailVO(orderVo, EntityVOConverter.toRepairCategoryVO(categories.selectById(order.getCategoryId())),
                attachmentVos, EntityVOConverter.toRepairRecordVOList(records.selectList(
                        Wrappers.<RepairRecord>lambdaQuery().eq(RepairRecord::getOrderId, id))), EntityVOConverter.toRepairCommentVOList(comments.selectList(
                        Wrappers.<RepairComment>lambdaQuery().eq(RepairComment::getOrderId, id).orderByDesc(RepairComment::getIsPinned)
                                .orderByAsc(RepairComment::getCreateTime))), logVos, evaluationVo);
    }

    /**
     * 创建草稿工单。
     * requestId 作为幂等键，网络重试或重复点击时直接返回已有草稿，避免重复创建。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderVO create(RepairOrderEditDTO body) {
        CurrentUser me = SecurityUtils.current();
        if (!List.of("STUDENT", "TEACHER").contains(me.roleCode())) {
            throw BusinessException.forbidden("仅学生和教师可提交报修");
        }
        String request = required(body.getRequestId(), "requestId");
        RepairOrder existing = orders.selectOne(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getRequestId, request));
        if (existing != null) {
            return EntityVOConverter.toRepairOrderVO(existing);
        }
        SysUser user = users.selectById(me.userId());
        RepairOrder order = new RepairOrder();
        order.setOrderNo("RO" + System.currentTimeMillis());
        order.setRequestId(request);
        order.setReporterId(me.userId());
        order.setReporterRoleId(user.getRoleId());
        order.setReporterNickname(user.getNickName());
        order.setReporterAvatar(user.getAvatar());
        order.setRepairType(RepairType.NORMAL.getCode());
        applyEditable(order, body, RepairStatus.DRAFT);
        order.setStatus(RepairStatus.DRAFT.getCode());
        order.setUnresolvedCount(0);
        order.setVersion(0);
        orders.insert(order);
        log(order, null, RepairStatus.DRAFT.getCode(), "创建草稿");
        return EntityVOConverter.toRepairOrderVO(order);
    }

    /**
     * 修改处于"草稿"或"被驳回"状态的工单内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderVO update(Long id, RepairOrderEditDTO body) {
        RepairOrder order = requireReporter(id);
        RepairStatus status = RepairStatus.of(order.getStatus());
        if (!List.of(RepairStatus.DRAFT, RepairStatus.PENDING_ACCEPT, RepairStatus.PENDING_DISPATCH).contains(status)) {
            throw BusinessException.conflict("当前状态不可编辑");
        }
        int version = required(body.getVersion(), "version");
        applyEditable(order, body, status);
        int updated = orders.update(order, Wrappers.<RepairOrder>lambdaUpdate().eq(RepairOrder::getOrderId, id).eq(RepairOrder::getReporterId, SecurityUtils.current().userId())
                .in(RepairOrder::getStatus, RepairStatus.DRAFT.getCode(), RepairStatus.PENDING_DISPATCH.getCode(), RepairStatus.PENDING_ACCEPT.getCode())
                        .eq(RepairOrder::getVersion, version).setSql("version = version + 1"));
        if (updated != 1) {
            throw BusinessException.conflict("工单已更新，请刷新后重试");
        }
        RepairOrder saved = orders.selectById(id);
        log(saved, saved.getStatus(), saved.getStatus(), "编辑工单");
        return EntityVOConverter.toRepairOrderVO(saved);
    }

    /**
     * 提交前检测是否疑似重复报修
     */
    @Override
    public DuplicateRepairCheckVO checkDuplicate(Long id) {
        RepairOrder order = requireReporter(id);
        return duplicateRepairDetection.detect(order, SecurityUtils.current().userId());
    }

    /**
     * 报修人正式提交工单（将工单从"草稿"状态推送到抢单大厅）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderSubmitResultVO submit(Long id, VersionDTO body) {
        RepairOrder order = requireReporter(id);
        if (order.getStatus() != RepairStatus.DRAFT.getCode()) {
            throw BusinessException.conflict("仅草稿可提交");
        }
        long count = attachments.selectCount(Wrappers.<RepairAttachment>lambdaQuery().eq(RepairAttachment::getOrderId, id).isNull(RepairAttachment::getRecordId));
        if (count < 1 || count > 5) {
            throw BusinessException.badRequest("提交时必须有 1 至 5 张现场图片");
        }
        DuplicateRepairCheckVO duplicateCheck = duplicateRepairDetection.detect(order, SecurityUtils.current().userId());
        if (RepairType.ASSET.getCode().equals(order.getRepairType())) {
            assetService.claimAssetOnSubmit(order);
            orders.updateById(order);
        }
        int nextStatus = hasRepairer(order.getCategoryId()) ? RepairStatus.PENDING_ACCEPT.getCode() : RepairStatus.PENDING_DISPATCH.getCode();
        transition(order, required(body.getVersion(), "version"), nextStatus, null, "提交报修");
        if (Boolean.TRUE.equals(duplicateCheck.getSuspected())) {
            RepairOrder saved = orders.selectById(id);
            saved.setSuspectedDuplicate(1);
            saved.setDuplicateReason(duplicateCheck.getDuplicateReason());
            orders.updateById(saved);
        }
        RepairOrderSubmitResultVO result = new RepairOrderSubmitResultVO();
        result.setOrder(EntityVOConverter.toRepairOrderVO(orders.selectById(id)));
        result.setDuplicateReminder(duplicateCheck.getReporterReminder());
        return result;
    }

    /**
     * 维修师傅抢单。
     * 真正的并发保护在 Mapper 条件更新中完成：status + version 同时匹配才允许状态变化。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id, VersionDTO body) {
        CurrentUser me = SecurityUtils.current();
        requireAvailableRepairer(me.userId());
        RepairOrder order = requireOrder(id);
        if (!hasCapability(me.userId(), order.getCategoryId())) {
            throw BusinessException.forbidden("不具备该故障类型维修能力");
        }
        int version = required(body.getVersion(), "version");
        transition(order, version, RepairStatus.ACCEPTED.getCode(), me.userId(), "维修师傅接单");
        RepairAssignment assignment = new RepairAssignment();
        assignment.setOrderId(id);
        assignment.setRepairerId(me.userId());
        assignment.setStatus(0);
        assignment.setAssignmentSource(0);
        assignments.insert(assignment);
        repairCycleService.startWorkCycle(id, me.userId());
        assetService.onOrderAccepted(orders.selectById(id));
        notify(order.getReporterId(), id, "工单已接单", "维修师傅已接取工单", NotificationType.ORDER_STATUS);
    }

    /**
     * 【维修师傅专属】标记工单开始处理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id, VersionDTO body) {
        RepairOrder order = requireRepairer(id, RepairStatus.ACCEPTED);
        transition(order, required(body.getVersion(), "version"), RepairStatus.PROCESSING.getCode(), order.getCurrentRepairerId(), "开始处理");
        notify(order.getReporterId(), id, "工单处理中", "维修师傅已开始处理", NotificationType.ORDER_STATUS);
    }

    /**
     * 【维修师傅专属】提交维修结果（修好或无法修复）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void result(Long id, RepairResultDTO body) {
        RepairOrder order = requireRepairer(id, RepairStatus.PROCESSING);
        RepairRecord record = new RepairRecord();
        record.setOrderId(id);
        record.setRepairerId(SecurityUtils.current().userId());
        record.setResultDescription(body.getDescription());
        record.setAttemptNo(order.getUnresolvedCount() + 1);
        records.insert(record);
        repairCycleService.endWorkCycle(id);
        transition(order, required(body.getVersion(), "version"), RepairStatus.PENDING_CONFIRM.getCode(), order.getCurrentRepairerId(), "提交维修结果");
        repairCycleService.startConfirmationCycle(id, order.getReporterId());
        notify(order.getReporterId(), id, "请确认维修结果", "维修师傅已提交维修结果", NotificationType.ORDER_STATUS);
    }

    /**
     * 报修人确认维修完成
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, VersionDTO body) {
        RepairOrder order = requireReporter(id);
        if (order.getStatus() != RepairStatus.PENDING_CONFIRM.getCode()) {
            throw BusinessException.conflict("当前状态不可确认");
        }
        int version = required(body.getVersion(), "version");
        if (orders.confirmComplete(id, version) != 1) {
            throw BusinessException.conflict("工单状态或版本已变化");
        }
        repairCycleService.endConfirmationCycle(id);
        RepairAssignment assignment = currentAssignment(id);
        if (assignment != null) {
            assignment.setStatus(2);
            assignments.updateById(assignment);
        }
        RepairOrder updated = orders.selectById(id);
        log(updated, RepairStatus.PENDING_CONFIRM.getCode(), RepairStatus.COMPLETED.getCode(), "报修人确认完成");
        assetService.onOrderEnded(updated);
        notify(order.getCurrentRepairerId(), id, "工单已完成", "报修人已确认完成", NotificationType.ORDER_STATUS);
    }

    /**
     * 报修人反馈未解决。
     * 第五次未解决进入待仲裁状态；之前的反馈回到处理中，继续由当前维修师傅处理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unresolved(Long id, VersionDTO body) {
        int version = required(body.getVersion(), "version");
        RepairOrder order = requireReporter(id);
        if (order.getAutoCompletedTime() != null) {
            throw BusinessException.conflict("自动完成工单不可反馈未解决");
        }
        if (order.getStatus() != RepairStatus.PENDING_CONFIRM.getCode() || order.getVersion() != version) {
            throw BusinessException.conflict("当前状态不可反馈");
        }
        Long repairerId = order.getCurrentRepairerId();
        if (orders.feedbackUnresolved(id, SecurityUtils.current().userId(), version) != 1) {
            throw BusinessException.conflict("工单状态或版本已变化");
        }
        repairCycleService.endConfirmationCycle(id);
        RepairOrder updated = orders.selectById(id);
        log(updated, RepairStatus.PENDING_CONFIRM.getCode(), updated.getStatus(), "反馈未解决");
        if (updated.getStatus() == RepairStatus.PROCESSING.getCode()) {
            repairCycleService.startWorkCycle(id, repairerId);
        }
        notify(repairerId, id, "报修人反馈未解决", "工单已重新进入处理中", NotificationType.ORDER_STATUS);
    }

    /**
     * 【维修师傅专属】退回工单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnOrder(Long id, RepairReturnDTO body) {
        RepairOrder order = requireRepairer(id, RepairStatus.ACCEPTED, RepairStatus.PROCESSING);
        RepairAssignment assignment = currentAssignment(id);
        if (assignment != null) {
            assignment.setStatus(1);
            assignment.setReturnReason(body.getReason());
            assignments.updateById(assignment);
        }
        repairCycleService.endWorkCycle(id);
        int nextStatus = hasRepairerExcluding(order.getCategoryId(), SecurityUtils.current().userId()) ? RepairStatus.PENDING_ACCEPT.getCode() : RepairStatus.PENDING_DISPATCH.getCode();
        transition(order, required(body.getVersion(), "version"), nextStatus, null, body.getReason());
        systemComment(id, "维修师傅退回工单：" + body.getReason());
        notify(order.getReporterId(), id, "工单已退回", body.getReason(), NotificationType.ORDER_STATUS);
    }

    /**
     * 报修人将待匹配或待接单工单撤回为草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawToDraft(Long id, VersionDTO body) {
        RepairOrder order = requireReporter(id);
        RepairStatus status = RepairStatus.of(order.getStatus());
        if (!List.of(RepairStatus.PENDING_DISPATCH, RepairStatus.PENDING_ACCEPT).contains(status)) {
            throw BusinessException.conflict("当前状态不可撤回");
        }
        int fromStatus = order.getStatus();
        int version = required(body.getVersion(), "version");
        if (orders.withdrawToDraft(id, SecurityUtils.current().userId(), version) != 1) {
            throw BusinessException.conflict("工单状态或版本已变化");
        }
        RepairOrder updated = orders.selectById(id);
        assetService.releaseOnWithdrawToDraft(updated);
        log(updated, fromStatus, RepairStatus.DRAFT.getCode(), "撤回为草稿");
    }

    /**
     * 报修人将已驳回工单重新转为草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectedToDraft(Long id, VersionDTO body) {
        RepairOrder order = requireReporter(id);
        if (order.getStatus() != RepairStatus.REJECTED.getCode()) {
            throw BusinessException.conflict("仅已驳回工单可转为草稿");
        }
        int version = required(body.getVersion(), "version");
        if (orders.rejectedToDraft(id, SecurityUtils.current().userId(), version) != 1) {
            throw BusinessException.conflict("工单状态或版本已变化");
        }
        RepairOrder updated = orders.selectById(id);
        log(updated, RepairStatus.REJECTED.getCode(), RepairStatus.DRAFT.getCode(), "驳回后重新编辑");
    }

    // 报修人对自动完成工单在七天内申请仲裁。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestAutoCompleteArbitration(Long id, AutoCompleteArbitrationDTO body) {
        RepairOrder order = requireReporter(id);
        if (order.getStatus() != RepairStatus.COMPLETED.getCode()) {
            throw BusinessException.conflict("仅已完成工单可申请仲裁");
        }
        if (order.getAutoCompletedTime() == null) {
            throw BusinessException.conflict("仅自动完成工单可申请仲裁");
        }
        if (order.getAutoCompletedTime().plusDays(7).isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("已超过自动完成后七天的申请期限");
        }
        String reason = required(body.getReason(), "reason");
        transition(order, required(body.getVersion(), "version"), RepairStatus.PENDING_ARBITRATION.getCode(), order.getCurrentRepairerId(), reason);
        systemComment(id, "报修人申请自动完成仲裁：" + reason);
        // 通知所有启用管理员进行仲裁处理
        notifyAdmins(id, "工单 " + order.getOrderNo() + " 报修人申请自动完成仲裁，请及时处理。原因：" + reason);
    }

    /**
     * 对工单发表评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairCommentVO comment(Long id, CommentDTO body) {
        RepairOrder order = requireVisible(id);
        CurrentUser me = SecurityUtils.current();
        if (!me.userId().equals(order.getReporterId()) && !me.userId().equals(order.getCurrentRepairerId())) {
            throw BusinessException.forbidden("无权评论");
        }
        RepairComment comment = new RepairComment();
        comment.setOrderId(id);
        comment.setAuthorId(me.userId());
        comment.setCommentType(0);
        comment.setContent(required(body.getContent(), "content"));
        comment.setIsPinned(0);
        comment.setIsWithdrawn(0);
        comments.insert(comment);
        return EntityVOConverter.toRepairCommentVO(comment);
    }

    /**
     * 撤回评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawComment(Long id) {
        RepairComment comment = comments.selectById(id);
        if (comment == null || !SecurityUtils.current().userId().equals(comment.getAuthorId())) {
            throw BusinessException.forbidden("无权撤回");
        }
        if (comment.getCreateTime().plusHours(12).isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("评论已超过可撤回时间");
        }
        comment.setIsWithdrawn(1);
        comment.setContent("该评论已撤回");
        comment.setWithdrawTime(LocalDateTime.now());
        comments.updateById(comment);
    }

    /**
     * 对已完成的工单进行服务评价
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long id, EvaluationDTO body) {
        RepairOrder order = requireReporter(id);
        if (order.getStatus() != RepairStatus.COMPLETED.getCode()) {
            throw BusinessException.conflict("仅已完成工单可评价");
        }
        RepairEvaluation existing = evaluations.selectOne(Wrappers.<RepairEvaluation>lambdaQuery()
                .eq(RepairEvaluation::getOrderId, id)
                .eq(RepairEvaluation::getDeleteState, 0));
        if (existing != null) {
            throw BusinessException.conflict("该工单已评价");
        }
        int star = required(body.getStar(), "star");
        if (star < 1 || star > 5) {
            throw BusinessException.badRequest("评分必须为 1 至 5 星");
        }
        String content = body.getContent() == null ? null : body.getContent().trim();
        if (content != null && content.isBlank()) {
            content = null;
        }
        if (content != null && content.length() > 50) {
            throw BusinessException.badRequest("评价内容不能超过50字");
        }
        RepairEvaluation evaluation = new RepairEvaluation();
        evaluation.setOrderId(id);
        evaluation.setRepairerId(order.getCurrentRepairerId());
        evaluation.setStar(star);
        evaluation.setContent(content);
        evaluations.insert(evaluation);
    }

    /**
     * 评价完成后的追评接口
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUp(Long id, FollowUpDTO body) {
        requireReporter(id);
        RepairEvaluation evaluation = evaluations.selectOne(Wrappers.<RepairEvaluation>lambdaQuery().eq(RepairEvaluation::getOrderId, id));
        if (evaluation == null || evaluation.getFollowUpContent() != null) {
            throw BusinessException.conflict("不可追评");
        }
        evaluation.setFollowUpContent(required(body.getContent(), "content"));
        evaluation.setFollowUpTime(LocalDateTime.now());
        evaluations.updateById(evaluation);
    }

    // 为工单上传附件
    @Override
    public RepairAttachmentVO uploadAttachment(Long orderId, Long recordId, MultipartFile file) throws Exception {
        validateImage(file);
        String name = file.getOriginalFilename();
        String extension = name != null && name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "jpg";
        RepairAttachmentVO vo = EntityVOConverter.toRepairAttachmentVO(saveAttachment(orderId, recordId, oss.upload(extension, file.getInputStream())));
        fillSignedUrls(List.of(vo));
        return vo;
    }

    /**
     * 管理员强制流转工单状态（如驳回、关闭或仲裁）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminTransition(Long id, int version, int to, String reason) {
        RepairOrder order = requireOrder(id);
        transition(order, version, to, order.getCurrentRepairerId(), reason);
        RepairOrder updated = orders.selectById(id);
        if (to == RepairStatus.COMPLETED.getCode()) {
            updated.setCompletionTime(LocalDateTime.now());
            orders.updateById(updated);
            updated = orders.selectById(id);
        }
        if (to == RepairStatus.COMPLETED.getCode() || to == RepairStatus.REJECTED.getCode() || to == RepairStatus.CLOSED.getCode()) {
            assetService.onOrderEnded(updated);
        }
        systemComment(id, reason);
        notify(order.getReporterId(), id, "工单状态已更新", reason, NotificationType.ORDER_STATUS);
    }

    /**
     * 管理员手动派单，将待匹配或待接单工单指定给维修师傅。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDispatch(Long id, AdminDispatchDTO body) {
        CurrentUser me = SecurityUtils.current();
        if (!"ADMIN".equals(me.roleCode())) {
            throw BusinessException.forbidden("仅管理员可手动派单");
        }
        RepairOrder order = requireOrder(id);
        RepairStatus status = RepairStatus.of(order.getStatus());
        if (!List.of(RepairStatus.PENDING_DISPATCH, RepairStatus.PENDING_ACCEPT).contains(status)) {
            throw BusinessException.conflict("当前状态不可派单");
        }
        int version = required(body.getVersion(), "version");
        if (!Integer.valueOf(version).equals(order.getVersion())) {
            throw BusinessException.conflict("工单已更新，请刷新后重试");
        }
        Long repairerId = required(body.getRepairerId(), "repairerId");
        requireActiveRepairer(repairerId);
        String dispatchNote = required(body.getDispatchNote(), "dispatchNote");
        boolean capable = hasCapability(repairerId, order.getCategoryId());
        String mismatchReason = body.getCapabilityMismatchReason();
        if (!capable && (mismatchReason == null || mismatchReason.isBlank())) {
            throw BusinessException.badRequest("能力不匹配时必须填写原因");
        }
        if (orders.adminDispatch(id, RepairStatus.ACCEPTED.getCode(), version, repairerId) != 1) {
            throw BusinessException.conflict("工单状态或版本已变化");
        }
        RepairAssignment assignment = new RepairAssignment();
        assignment.setOrderId(id);
        assignment.setRepairerId(repairerId);
        assignment.setStatus(0);
        assignment.setAssignmentSource(1);
        assignment.setOperatorId(me.userId());
        assignment.setDispatchNote(dispatchNote);
        if (!capable) {
            assignment.setCapabilityMismatchReason(mismatchReason);
        }
        assignments.insert(assignment);
        repairCycleService.startWorkCycle(id, repairerId);
        RepairOrder updated = orders.selectById(id);
        log(updated, status.getCode(), RepairStatus.ACCEPTED.getCode(), "管理员手动派单");
        StringBuilder comment = new StringBuilder("管理员手动派单：").append(dispatchNote);
        if (!capable) {
            comment.append("；能力不匹配原因：").append(mismatchReason);
        }
        systemComment(id, comment.toString());
        RepairOrder dispatched = orders.selectById(id);
        assetService.onOrderAccepted(dispatched);
        notify(order.getReporterId(), id, "工单已派单", dispatchNote, NotificationType.ORDER_STATUS);
        notify(repairerId, id, "管理员已派单", dispatchNote, NotificationType.ORDER_STATUS);
        writeOperationLog(me.userId(), "DISPATCH_ORDER", "ORDER", id, "管理员手动派单给维修师傅 " + repairerId + "：" + dispatchNote);
    }

    /**
     * 查询管理员派单候选维修师傅列表。
     */
    @Override
    public List<RepairerCandidateVO> dispatchCandidates(Long id) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可查看派单候选");
        }
        RepairOrder order = requireOrder(id);
        RepairStatus status = RepairStatus.of(order.getStatus());
        if (!List.of(RepairStatus.PENDING_DISPATCH, RepairStatus.PENDING_ACCEPT).contains(status)) {
            throw BusinessException.conflict("当前状态不可派单");
        }
        SysRole repairerRole = requireRole();
        List<SysUser> repairers = users.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getRoleId, repairerRole.getRoleId()).eq(SysUser::getAccountStatus, 0).eq(SysUser::getActivationStatus, 1));
        List<RepairerCandidateVO> result = new ArrayList<>();
        for (SysUser repairer : repairers) {
            RepairerCandidateVO vo = new RepairerCandidateVO();
            vo.setUserId(repairer.getUserId());
            vo.setUserNo(repairer.getUserNo());
            vo.setRealName(repairer.getRealName());
            vo.setHasCapability(hasCapability(repairer.getUserId(), order.getCategoryId()));
            BusyLevel busyLevel = BusyLevel.fromCount(countActiveOrders(repairer.getUserId()).intValue());
            vo.setBusyLevel(busyLevel.getCode());
            vo.setBusyLevelLabel(busyLevel.getLabel());
            result.add(vo);
        }
        return result;
    }

    /**
     * 查询指定故障类型的整体维修力量繁忙程度。
     */
    @Override
    public WorkforceSummaryVO workforceSummary(Long categoryId) {
        required(categoryId, "categoryId");
        SysRole repairerRole = requireRole();
        List<Long> repairerIds = capabilities.selectList(Wrappers.<RepairCapability>lambdaQuery().eq(RepairCapability::getCategoryId, categoryId))
                .stream().map(RepairCapability::getRepairerId).distinct().filter(repairerId -> isActiveRepairer(repairerId, repairerRole.getRoleId())).toList();
        WorkforceSummaryVO vo = new WorkforceSummaryVO();
        vo.setHasRepairer(!repairerIds.isEmpty());
        if (repairerIds.isEmpty()) {
            BusyLevel idle = BusyLevel.IDLE;
            vo.setBusyLevel(idle.getCode());
            vo.setBusyLevelLabel(idle.getLabel());
            return vo;
        }
        double average = repairerIds.stream().mapToLong(this::countActiveOrders).average().orElse(0D);
        BusyLevel level = BusyLevel.fromCount((int) Math.round(average));
        vo.setBusyLevel(level.getCode());
        vo.setBusyLevelLabel(level.getLabel());
        return vo;
    }

    /**
     * 查询当前维修师傅自身的繁忙程度编码。
     */
    @Override
    public String currentRepairerBusyLevel() {
        CurrentUser me = SecurityUtils.current();
        if (!"REPAIRER".equals(me.roleCode())) {
            throw BusinessException.forbidden("仅维修师傅可查询繁忙程度");
        }
        return BusyLevel.fromCount(countActiveOrders(me.userId()).intValue()).getCode();
    }

    private RepairAttachment saveAttachment(Long orderId, Long recordId, String key) {
        RepairOrder order = requireVisible(orderId);
        CurrentUser me = SecurityUtils.current();
        if (recordId == null && !me.userId().equals(order.getReporterId())) {
            throw BusinessException.forbidden("无权上传现场图片");
        }
        if (recordId != null && !me.userId().equals(order.getCurrentRepairerId())) {
            throw BusinessException.forbidden("无权上传维修图片");
        }
        RepairAttachment attachment = new RepairAttachment();
        attachment.setOrderId(orderId);
        attachment.setRecordId(recordId);
        attachment.setObjectKey(key);
        attachment.setUploaderId(me.userId());
        attachments.insert(attachment);
        return attachment;
    }

    private void validateImage(MultipartFile file) throws Exception {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024 || ImageIO.read(file.getInputStream()) == null) {
            throw BusinessException.badRequest("仅支持 20MB 内的有效图片");
        }
    }

    // 状态机统一入口
    private void transition(RepairOrder order, int version, int to, Long repairer, String remark) {
        if (order == null || orders.transition(order.getOrderId(), order.getStatus(), to, version, repairer) != 1) {
            throw BusinessException.conflict("工单状态或版本已变化");
        }
        log(order, order.getStatus(), to, remark);
    }

    private void log(RepairOrder order, Integer from, Integer to, String remark) {
        RepairOrderLog log = new RepairOrderLog();
        log.setOrderId(order.getOrderId());
        log.setOperatorId(SecurityUtils.current().userId());
        log.setAction(to == null ? 0 : to);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setRemark(remark);
        logs.insert(log);
    }

    private void writeOperationLog(Long operatorId, String type, String targetType, Long targetId, String description) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(operatorId);
        log.setOperationType(type);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    // 向指定用户发送站内信通知
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

    // 向所有启用管理员批量发送站内通知。
    private void notifyAdmins(Long orderId, String content) {
        SysRole adminRole = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, "ADMIN"));
        if (adminRole == null) {
            return;
        }
        List<SysUser> admins = users.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getRoleId, adminRole.getRoleId()).eq(SysUser::getAccountStatus, 0).eq(SysUser::getActivationStatus, 1));
        for (SysUser admin : admins) {
            notify(admin.getUserId(), orderId, "工单仲裁申请", content, NotificationType.ARBITRATION_REQUEST);
        }
    }

    // 对每个附件 VO 调用 OssService 生成预签名 URL 并写入 signedUrl 字段。
    private void fillSignedUrls(List<RepairAttachmentVO> vos) {
        for (RepairAttachmentVO vo : vos) {
            if (vo.getObjectKey() == null) {
                continue;
            }
            try {
                URL url = oss.signedUrl(vo.getObjectKey());
                if (url != null) {
                    vo.setSignedUrl(url.toString());
                }
            } catch (Exception ignored) {
                // OSS 未配置或网络不通时不中断主流程，前端使用 error 插槽兜底
            }
        }
    }

    private void systemComment(Long id, String text) {
        RepairComment comment = new RepairComment();
        comment.setOrderId(id);
        comment.setCommentType(1);
        comment.setContent(text);
        comment.setIsPinned(1);
        comment.setIsWithdrawn(0);
        comments.insert(comment);
    }

    private RepairOrder requireVisible(Long id) {
        RepairOrder order = requireOrder(id);
        CurrentUser me = SecurityUtils.current();
        if ("ADMIN".equals(me.roleCode()) || me.userId().equals(order.getReporterId()) || me.userId().equals(order.getCurrentRepairerId())) {
            return order;
        }
        if ("REPAIRER".equals(me.roleCode()) && order.getStatus() == RepairStatus.PENDING_ACCEPT.getCode()) {
            return order;
        }
        throw BusinessException.forbidden("无权查看工单");
    }

    private RepairOrder requireReporter(Long id) {
        RepairOrder order = requireOrder(id);
        if (!SecurityUtils.current().userId().equals(order.getReporterId())) {
            throw BusinessException.forbidden("仅报修人可操作");
        }
        return order;
    }

    private RepairOrder requireRepairer(Long id, RepairStatus... allowed) {
        RepairOrder order = requireOrder(id);
        if (!SecurityUtils.current().userId().equals(order.getCurrentRepairerId()) || Arrays.stream(allowed).noneMatch(status -> status.getCode() == order.getStatus())) {
            throw BusinessException.forbidden("当前维修师傅不可执行此操作");
        }
        return order;
    }

    private RepairOrder requireOrder(Long id) {
        RepairOrder order = orders.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("工单不存在");
        }
        return order;
    }

    private void requireActiveRepairer(Long repairerId) {
        SysUser repairer = requireRepairerAccount(repairerId);
        if (repairer.getAccountStatus() != 0 || repairer.getActivationStatus() != 1) {
            throw BusinessException.badRequest("目标维修师傅账号不可用");
        }
    }

    private void requireAvailableRepairer(Long repairerId) {
        SysUser repairer = requireRepairerAccount(repairerId);
        if (repairer.getAccountStatus() != 0 || repairer.getActivationStatus() != 1) {
            throw BusinessException.badRequest("维修师傅账号不可用");
        }
        if (!isAcceptingAvailable(repairer)) {
            throw BusinessException.conflict("当前处于暂停接单状态，不能查看或接取新工单");
        }
    }

    private SysUser requireRepairerAccount(Long repairerId) {
        SysUser repairer = users.selectById(repairerId);
        if (repairer == null) {
            throw BusinessException.badRequest("维修师傅不存在");
        }
        SysRole role = roles.selectById(repairer.getRoleId());
        if (role == null || !"REPAIRER".equals(role.getRoleName())) {
            throw BusinessException.badRequest("目标用户不是维修师傅");
        }
        return repairer;
    }

    private boolean isAcceptingAvailable(SysUser repairer) {
        String state = repairer.getAcceptingState();
        return state == null || RepairerAcceptingState.AVAILABLE.getCode().equals(state);
    }

    private SysRole requireRole() {
        SysRole role = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, "REPAIRER"));
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        return role;
    }

    private RepairAssignment currentAssignment(Long orderId) {
        return assignments.selectOne(Wrappers.<RepairAssignment>lambdaQuery().eq(RepairAssignment::getOrderId, orderId)
                .eq(RepairAssignment::getStatus, 0).last("LIMIT 1"));
    }

    private boolean hasRepairer(Long category) {
        Long repairerRoleId = requireRole().getRoleId();
        return capabilities.selectList(Wrappers.<RepairCapability>lambdaQuery().eq(RepairCapability::getCategoryId, category))
                .stream().anyMatch(capability -> isActiveRepairer(capability.getRepairerId(), repairerRoleId));
    }

    private boolean hasRepairerExcluding(Long category, Long excluded) {
        return capabilities.selectList(Wrappers.<RepairCapability>lambdaQuery().eq(RepairCapability::getCategoryId, category).ne(RepairCapability::getRepairerId, excluded)).stream()
                .anyMatch(capability -> {SysUser user = users.selectById(capability.getRepairerId());return user != null && user.getAccountStatus() == 0;});
    }

    private boolean hasCapability(Long user, Long category) {
        return capabilities.selectCount(Wrappers.<RepairCapability>lambdaQuery().eq(RepairCapability::getRepairerId, user).eq(RepairCapability::getCategoryId, category)) > 0;
    }

    private boolean isActiveRepairer(Long userId, Long repairerRoleId) {
        SysUser user = users.selectById(userId);
        return user != null && repairerRoleId.equals(user.getRoleId()) && user.getAccountStatus() == 0 && user.getActivationStatus() == 1 && isAcceptingAvailable(user);
    }

    private Long countActiveOrders(Long repairerId) {
        return orders.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getCurrentRepairerId, repairerId).in(RepairOrder::getStatus, RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode()));
    }

    private void applyEditable(RepairOrder order, RepairOrderEditDTO body, RepairStatus status) {
        order.setTitle(required(body.getTitle(), "title"));
        order.setDescription(required(body.getDescription(), "description"));
        order.setCategoryId(required(body.getCategoryId(), "categoryId"));
        order.setContactPhone(required(body.getContactPhone(), "contactPhone"));
        if (status == RepairStatus.DRAFT) {
            String repairType = body.getRepairType() == null ? RepairType.NORMAL.getCode() : body.getRepairType();
            assetService.applyAssetDraft(order, repairType, body.getAssetId());
        }
        if (!RepairType.ASSET.getCode().equals(order.getRepairType())) {
            LocationSnapshot snapshot = locationService.resolveSnapshot(required(body.getCampusId(), "campusId"), body.getBuildingId(), body.getLocationDetail());
            order.setCampusId(snapshot.getCampusId());
            order.setCampus(snapshot.getCampusName());
            order.setCampusDescriptionSnapshot(snapshot.getCampusDescription());
            order.setBuildingId(snapshot.getBuildingId());
            order.setBuilding(snapshot.getBuildingName());
            order.setBuildingDescriptionSnapshot(snapshot.getBuildingDescription());
            order.setFloor(body.getFloor());
            order.setRoom(body.getRoom());
            order.setLocationDetail(required(body.getLocationDetail(), "locationDetail"));
            return;
        }
        if (status == RepairStatus.DRAFT) {
            order.setFloor(body.getFloor() != null ? body.getFloor() : order.getFloor());
            order.setRoom(body.getRoom() != null ? body.getRoom() : order.getRoom());
            if (body.getLocationDetail() != null && !body.getLocationDetail().isBlank()) {
                order.setLocationDetail(body.getLocationDetail());
            }
        }
    }

    private boolean hasAdminOnlyFilter(RepairOrderQueryDTO query) {
        return (query.getReporterKeyword() != null && !query.getReporterKeyword().isBlank())
                || (query.getRepairerKeyword() != null && !query.getRepairerKeyword().isBlank()) || Boolean.TRUE.equals(query.getLongStagnant());
    }

    private LambdaQueryWrapper<RepairOrder> buildSearchWrapper(CurrentUser me, boolean admin, RepairOrderQueryDTO query) {
        LambdaQueryWrapper<RepairOrder> wrapper = Wrappers.lambdaQuery();
        applyRoleScope(wrapper, me, admin);
        applyQuickFilter(wrapper, query.getQuickFilter(), me);
        if (query.getOrderNo() != null && !query.getOrderNo().isBlank()) {
            wrapper.eq(RepairOrder::getOrderNo, query.getOrderNo().trim());
        }
        if (query.getTitleKeyword() != null && !query.getTitleKeyword().isBlank()) {
            wrapper.like(RepairOrder::getTitle, query.getTitleKeyword().trim());
        }
        if (query.getCreateTimeFrom() != null) {
            wrapper.ge(RepairOrder::getCreateTime, query.getCreateTimeFrom());
        }
        if (query.getCreateTimeTo() != null) {
            wrapper.le(RepairOrder::getCreateTime, query.getCreateTimeTo());
        }
        if (query.getCompletionTimeFrom() != null) {
            wrapper.ge(RepairOrder::getCompletionTime, query.getCompletionTimeFrom());
        }
        if (query.getCompletionTimeTo() != null) {
            wrapper.le(RepairOrder::getCompletionTime, query.getCompletionTimeTo());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RepairOrder::getStatus, query.getStatus());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(RepairOrder::getCategoryId, query.getCategoryId());
        }
        if (query.getCampusId() != null) {
            wrapper.eq(RepairOrder::getCampusId, query.getCampusId());
        }
        if (query.getBuildingId() != null) {
            wrapper.eq(RepairOrder::getBuildingId, query.getBuildingId());
        }
        if (query.getAssetNo() != null && !query.getAssetNo().isBlank()) {
            wrapper.eq(RepairOrder::getAssetNoSnapshot, query.getAssetNo().trim());
        }
        if (query.getAssetNameKeyword() != null && !query.getAssetNameKeyword().isBlank()) {
            wrapper.like(RepairOrder::getAssetNameSnapshot, query.getAssetNameKeyword().trim());
        }
        if (admin && query.getExportedFlag() != null) {
            wrapper.eq(RepairOrder::getExportedFlag, query.getExportedFlag());
        }
        if (admin && query.getSuspectedDuplicate() != null) {
            wrapper.eq(RepairOrder::getSuspectedDuplicate, query.getSuspectedDuplicate());
        }
        if (admin) {
            applyUserKeywordFilter(wrapper, query.getReporterKeyword(), true);
            applyUserKeywordFilter(wrapper, query.getRepairerKeyword(), false);
            if (Boolean.TRUE.equals(query.getLongStagnant())) {
                List<Long> stagnantOrderIds = repairCycleService.findLongStagnantOrderIds();
                if (stagnantOrderIds.isEmpty()) {
                    wrapper.eq(RepairOrder::getOrderId, -1L);
                } else {
                    wrapper.in(RepairOrder::getOrderId, stagnantOrderIds);
                    wrapper.in(RepairOrder::getStatus, RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode());
                }
            }
        }
        wrapper.orderByDesc(RepairOrder::getCreateTime);
        return wrapper;
    }

    private void applyRoleScope(LambdaQueryWrapper<RepairOrder> wrapper, CurrentUser me, boolean admin) {
        if (admin) {
            return;
        }
        if ("STUDENT".equals(me.roleCode()) || "TEACHER".equals(me.roleCode())) {
            wrapper.eq(RepairOrder::getReporterId, me.userId());
            return;
        }
        if ("REPAIRER".equals(me.roleCode())) {
            wrapper.eq(RepairOrder::getCurrentRepairerId, me.userId());
            return;
        }
        throw BusinessException.forbidden("无权查询工单");
    }

    private void applyQuickFilter(LambdaQueryWrapper<RepairOrder> wrapper, String quickFilter, CurrentUser me) {
        if (quickFilter == null || quickFilter.isBlank()) {
            return;
        }
        switch (quickFilter) {
            case "REPAIRER_PROCESSING" -> {
                requireQuickFilterRole(me, "REPAIRER");
                wrapper.in(RepairOrder::getStatus, RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode());
            }
            case "REPAIRER_PENDING_CONFIRM" -> {
                requireQuickFilterRole(me, "REPAIRER");
                wrapper.eq(RepairOrder::getStatus, RepairStatus.PENDING_CONFIRM.getCode());
            }
            case "REPAIRER_COMPLETED" -> {
                requireQuickFilterRole(me, "REPAIRER");
                wrapper.eq(RepairOrder::getStatus, RepairStatus.COMPLETED.getCode());
            }
            case "REPORTER_PROCESSING" -> {
                requireQuickFilterRole(me, "STUDENT", "TEACHER");
                wrapper.in(RepairOrder::getStatus, RepairStatus.PENDING_DISPATCH.getCode(),
                        RepairStatus.PENDING_ACCEPT.getCode(), RepairStatus.ACCEPTED.getCode(), RepairStatus.PROCESSING.getCode(), RepairStatus.PENDING_ARBITRATION.getCode());
            }
            case "REPORTER_PENDING_CONFIRM" -> {
                requireQuickFilterRole(me, "STUDENT", "TEACHER");
                wrapper.eq(RepairOrder::getStatus, RepairStatus.PENDING_CONFIRM.getCode());
            }
            case "REPORTER_ENDED" -> {
                requireQuickFilterRole(me, "STUDENT", "TEACHER");
                wrapper.in(RepairOrder::getStatus, RepairStatus.COMPLETED.getCode(), RepairStatus.REJECTED.getCode(), RepairStatus.CLOSED.getCode());
            }
            default -> throw BusinessException.badRequest("未知快捷筛选");
        }
    }

    private void requireQuickFilterRole(CurrentUser me, String... allowedRoles) {
        if (Arrays.stream(allowedRoles).noneMatch(role -> role.equals(me.roleCode()))) {
            throw BusinessException.forbidden("当前角色不可使用该快捷筛选");
        }
    }

    private void applyUserKeywordFilter(LambdaQueryWrapper<RepairOrder> wrapper, String keyword, boolean reporter) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        List<Long> userIds = findUserIdsByKeyword(keyword.trim());
        if (userIds.isEmpty()) {
            wrapper.eq(RepairOrder::getOrderId, -1L);
            return;
        }
        if (reporter) {
            wrapper.in(RepairOrder::getReporterId, userIds);
        } else {
            wrapper.in(RepairOrder::getCurrentRepairerId, userIds);
        }
    }

    private List<Long> findUserIdsByKeyword(String keyword) {
        return users.selectList(Wrappers.<SysUser>lambdaQuery().and(query -> query.eq(SysUser::getUserNo, keyword)
                        .or().like(SysUser::getRealName, keyword))).stream().map(SysUser::getUserId).toList();
    }

    private PageResult<RepairOrderVO> toEnrichedOrderPage(IPage<RepairOrder> page, CurrentUser me, boolean admin) {
        List<RepairOrderVO> records = page.getRecords().stream().map(EntityVOConverter::toRepairOrderVO).collect(Collectors.toList());
        if (admin) {
            enrichAdminListFields(records);
        }
        for (RepairOrderVO vo : records) {
            enrichOrderVo(vo, me, admin);
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void enrichAdminListFields(List<RepairOrderVO> records) {
        Set<Long> userIds = new HashSet<>();
        for (RepairOrderVO vo : records) {
            if (vo.getReporterId() != null) {
                userIds.add(vo.getReporterId());
            }
            if (vo.getCurrentRepairerId() != null) {
                userIds.add(vo.getCurrentRepairerId());
            }
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (SysUser user : users.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getUserId, userIds))) {
                userMap.put(user.getUserId(), user);
            }
        }
        for (RepairOrderVO vo : records) {
            SysUser reporter = userMap.get(vo.getReporterId());
            if (reporter != null) {
                vo.setReporterUserNo(reporter.getUserNo());
                vo.setReporterRealName(reporter.getRealName());
            }
            SysUser repairer = userMap.get(vo.getCurrentRepairerId());
            if (repairer != null) {
                vo.setRepairerUserNo(repairer.getUserNo());
                vo.setRepairerRealName(repairer.getRealName());
            }
            vo.setLongStagnant(repairCycleService.isLongStagnant(vo.getOrderId()));
        }
    }

    // 补充工单 VO 中需要额外计算或有权限限制的展示字段
    private void enrichOrderVo(RepairOrderVO vo, CurrentUser me, boolean admin) {
        if (!admin && !me.userId().equals(vo.getReporterId())) {
            vo.setContactPhone(null);
        }
        // 填充维修师傅展示字段：接单后对报修人、当前师傅、管理员可见
        if (vo.getCurrentRepairerId() != null && canSeeRepairerInfo(me, vo)) {
            SysUser repairer = users.selectById(vo.getCurrentRepairerId());
            if (repairer != null) {
                vo.setRepairerRealName(repairer.getRealName());
                vo.setRepairerUserNo(repairer.getUserNo());
            }
            BusyLevel level = BusyLevel.fromCount(countActiveOrders(vo.getCurrentRepairerId()).intValue());
            vo.setRepairerBusyLevel(level.getCode());
            vo.setRepairerBusyLevelLabel(level.getLabel());
        }
    }

    /** 判断当前用户是否有权查看维修师傅信息（接单人、报修人、管理员）。 */
    private boolean canSeeRepairerInfo(CurrentUser me, RepairOrderVO vo) {
        if ("ADMIN".equals(me.roleCode())) {
            return true;
        }
        if (me.userId().equals(vo.getReporterId())) {
            return true;
        }
        return me.userId().equals(vo.getCurrentRepairerId());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Long required(Long value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Integer required(Integer value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }
}