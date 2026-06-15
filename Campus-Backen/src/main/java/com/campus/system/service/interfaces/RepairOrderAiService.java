package com.campus.system.service.interfaces;

import com.campus.system.dto.RepairOrderLinkConfirmDTO;
import com.campus.system.vo.RepairOrderAiAnalysisVO;
import com.campus.system.vo.RepairOrderDuplicateDetailVO;

// 工单 AI 辅助业务接口
public interface RepairOrderAiService {

    RepairOrderDuplicateDetailVO loadDuplicateDetail(Long orderId);

    void confirmLink(RepairOrderLinkConfirmDTO body);

    void removeLink(Long linkId);

    RepairOrderAiAnalysisVO analyzeDispatch(Long orderId);
}
