package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.AutoCompleteArbitrationDTO;
import com.campus.system.dto.CommentDTO;
import com.campus.system.dto.EvaluationDTO;
import com.campus.system.dto.FollowUpDTO;
import com.campus.system.dto.RepairOrderEditDTO;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.dto.RepairResultDTO;
import com.campus.system.dto.RepairReturnDTO;
import com.campus.system.dto.VersionDTO;
import com.campus.system.service.interfaces.RepairOrderService;
import com.campus.system.service.interfaces.RepairerDashboardService;
import com.campus.system.service.interfaces.ReporterDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 报修工单
@RestController
public class RepairOrderController {

    @Autowired
    private RepairOrderService service;

    @Autowired
    private ReporterDashboardService reporterDashboardService;

    @Autowired
    private RepairerDashboardService repairerDashboardService;

    /**
     * 学生/教师报修首页看板
     */
    @GetMapping("/repair/reporter-dashboard")
    public ApiResponse<?> reporterDashboard(@RequestParam(required = false) Integer rangeDays) {
        return ApiResponse.ok(reporterDashboardService.dashboard(rangeDays));
    }

    /**
     * 维修师傅首页看板
     */
    @GetMapping("/repair/repairer-dashboard")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> repairerDashboard(@RequestParam(required = false) Integer rangeDays) {
        return ApiResponse.ok(repairerDashboardService.dashboard(rangeDays));
    }

    /**
     * 获取当前系统已启用的维修类目列表（例如：水电维修、网络报修等）
    */
    @GetMapping("/repair/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.ok(service.enabledCategories());
    }

    /**
     * 分页查询当前用户的报修工单列表（学生/教师查自己提的，维修师傅查自己接的）
    */
    @GetMapping("/repair/orders")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize, RepairOrderQueryDTO query) {
        return ApiResponse.ok(service.search(pageNum, pageSize, query == null ? new RepairOrderQueryDTO() : query));
    }

    /**
     * 获取某维修故障分类下的维修人员状态汇总（如在忙师傅数、空闲师傅数等）
    */
    @GetMapping("/repair/workforce-summary")
    public ApiResponse<?> workforceSummary(@RequestParam Long categoryId) {
        return ApiResponse.ok(service.workforceSummary(categoryId));
    }

    /**
     * 【维修师傅专属】获取当前维修师傅的繁忙程度级别（如忙碌、空闲）
    */
    @GetMapping("/repair/repairer/busy-level")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> repairerBusyLevel() {
        return ApiResponse.ok(service.currentRepairerBusyLevel());
    }

    /**
     * 【维修师傅专属】查询抢单大厅中可接单的工单列表
    */
    @GetMapping("/repair/orders/available")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> available(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) Long campusId, @RequestParam(required = false) Long buildingId,
                                    @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String titleKeyword) {
        return ApiResponse.ok(service.available(pageNum, pageSize, campusId, buildingId, categoryId, titleKeyword));
    }

    /**
     * 获取工单的详细信息（包含图片附件、处理记录、评论等关联信息）
    */
    @GetMapping("/repair/orders/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    /**
     * 创建（发起）一个新的报修工单（处于草稿状态）
    */
    @PostMapping("/repair/orders")
    public ApiResponse<?> create(@RequestBody RepairOrderEditDTO body) {
        return ApiResponse.ok(service.create(body));
    }

    /**
     * 修改处于“草稿”或“被驳回”状态的工单内容
    */
    @PutMapping("/repair/orders/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody RepairOrderEditDTO body) {
        return ApiResponse.ok(service.update(id, body));
    }

    /**
     * 提交前检测是否疑似重复报修
     */
    @PostMapping("/repair/orders/{id}/duplicate-check")
    public ApiResponse<?> checkDuplicate(@PathVariable Long id) {
        return ApiResponse.ok(service.checkDuplicate(id));
    }

    /**
     * 报修人正式提交工单（将工单从“草稿”状态推送到抢单大厅）
    */
    @PostMapping("/repair/orders/{id}/submit")
    public ApiResponse<?> submit(@PathVariable Long id, @RequestBody VersionDTO body) {
        return ApiResponse.ok(service.submit(id, body));
    }

    /**
     * 【维修师傅专属】接单（抢单）
    */
    @PostMapping("/repair/orders/{id}/accept")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> accept(@PathVariable Long id, @RequestBody VersionDTO body) {
        service.accept(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 【维修师傅专属】标记工单开始处理（例如到达现场开始维修）
    */
    @PostMapping("/repair/orders/{id}/start")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> start(@PathVariable Long id, @RequestBody VersionDTO body) {
        service.start(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 【维修师傅专属】提交维修结果（修好或无法修复）
    */
    @PostMapping("/repair/orders/{id}/result")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> result(@PathVariable Long id, @RequestBody RepairResultDTO body) {
        service.result(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 报修人确认维修完成
    */
    @PostMapping("/repair/orders/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable Long id, @RequestBody VersionDTO body) {
        service.confirm(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 报修人对维修结果不满意，标记为未解决，工单重新打回给师傅
    */
    @PostMapping("/repair/orders/{id}/unresolved")
    public ApiResponse<?> unresolved(@PathVariable Long id, @RequestBody VersionDTO body) {
        service.unresolved(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 【维修师傅专属】退回工单（例如发现该类目无法维修），工单重新回到大厅
    */
    @PostMapping("/repair/orders/{id}/return")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> returnOrder(@PathVariable Long id, @RequestBody RepairReturnDTO body) {
        service.returnOrder(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 报修人撤回工单到草稿状态
    */
    @PostMapping("/repair/orders/{id}/withdraw")
    public ApiResponse<?> withdraw(@PathVariable Long id, @RequestBody VersionDTO body) {
        service.withdrawToDraft(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 报修人将已被驳回的工单重新转为草稿
    */
    @PostMapping("/repair/orders/{id}/re-draft")
    public ApiResponse<?> reDraft(@PathVariable Long id, @RequestBody VersionDTO body) {
        service.rejectedToDraft(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 申请工单自动完工仲裁（当师傅提交完工后，若超过时限报修人未确认，可申请仲裁自动完工）
    */
    @PostMapping("/repair/orders/{id}/auto-complete-arbitration")
    public ApiResponse<?> autoCompleteArbitration(@PathVariable Long id, @RequestBody AutoCompleteArbitrationDTO body) {
        service.requestAutoCompleteArbitration(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 对工单发表评论（报修人催单或师傅反馈进度）
    */
    @PostMapping("/repair/orders/{id}/comments")
    public ApiResponse<?> comment(@PathVariable Long id, @RequestBody CommentDTO body) {
        return ApiResponse.ok(service.comment(id, body));
    }

    /**
     * 撤回评论（只能撤回自己发送且在允许撤回时间内的评论）
    */
    @PostMapping("/repair/comments/{id}/withdraw")
    public ApiResponse<?> withdraw(@PathVariable Long id) {
        service.withdrawComment(id);
        return ApiResponse.ok(null);
    }

    /**
     * 对已完成的工单进行服务评价（打星级并留言）
    */
    @PostMapping("/repair/orders/{id}/evaluation")
    public ApiResponse<?> evaluation(@PathVariable Long id, @RequestBody EvaluationDTO body) {
        service.evaluate(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 评价完成后的追评接口
    */
    @PostMapping("/repair/orders/{id}/evaluation/follow-up")
    public ApiResponse<?> followUp(@PathVariable Long id, @RequestBody FollowUpDTO body) {
        service.followUp(id, body);
        return ApiResponse.ok(null);
    }

    /**
     * 为工单上传附件（如故障图片）
    */
    @PostMapping("/repair/orders/{id}/attachments")
    public ApiResponse<?> upload(@PathVariable Long id, @RequestParam(required = false) Long recordId, @RequestPart MultipartFile file) throws Exception {
        return ApiResponse.ok(service.uploadAttachment(id, recordId, file));
    }
}
