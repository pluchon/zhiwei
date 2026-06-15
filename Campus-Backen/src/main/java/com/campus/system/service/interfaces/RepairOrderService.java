package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
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
import com.campus.system.vo.RepairAttachmentVO;
import com.campus.system.vo.RepairCategoryVO;
import com.campus.system.vo.RepairCommentVO;
import com.campus.system.vo.RepairOrderDetailVO;
import com.campus.system.vo.RepairOrderSubmitResultVO;
import com.campus.system.vo.RepairOrderVO;
import com.campus.system.vo.DuplicateRepairCheckVO;
import com.campus.system.vo.RepairerCandidateVO;
import com.campus.system.vo.WorkforceSummaryVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 报修工单业务接口。
 */
public interface RepairOrderService {

    List<RepairCategoryVO> enabledCategories();

    PageResult<RepairOrderVO> search(int page, int size, RepairOrderQueryDTO query);

    List<RepairOrderVO> listForExportByIds(List<Long> orderIds);

    PageResult<RepairOrderVO> available(int page, int size, Long campusId, Long buildingId, Long categoryId, String titleKeyword);

    RepairOrderDetailVO detail(Long id);

    RepairOrderVO create(RepairOrderEditDTO body);

    RepairOrderVO update(Long id, RepairOrderEditDTO body);

    DuplicateRepairCheckVO checkDuplicate(Long id);

    RepairOrderSubmitResultVO submit(Long id, VersionDTO body);

    void accept(Long id, VersionDTO body);

    void start(Long id, VersionDTO body);

    void result(Long id, RepairResultDTO body);

    void confirm(Long id, VersionDTO body);

    void unresolved(Long id, VersionDTO body);

    void returnOrder(Long id, RepairReturnDTO body);

    void withdrawToDraft(Long id, VersionDTO body);

    void rejectedToDraft(Long id, VersionDTO body);

    void requestAutoCompleteArbitration(Long id, AutoCompleteArbitrationDTO body);

    RepairCommentVO comment(Long id, CommentDTO body);

    void withdrawComment(Long id);

    void evaluate(Long id, EvaluationDTO body);

    void followUp(Long id, FollowUpDTO body);

    RepairAttachmentVO uploadAttachment(Long orderId, Long recordId, MultipartFile file) throws Exception;

    void adminTransition(Long id, int version, int to, String reason);

    void adminDispatch(Long id, AdminDispatchDTO body);

    List<RepairerCandidateVO> dispatchCandidates(Long id);

    WorkforceSummaryVO workforceSummary(Long categoryId);

    String currentRepairerBusyLevel();
}
