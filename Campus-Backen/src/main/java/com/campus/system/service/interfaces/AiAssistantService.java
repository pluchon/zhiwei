package com.campus.system.service.interfaces;

import com.campus.system.dto.AiAssistantChatDTO;
import com.campus.system.dto.AiExportConfirmDTO;
import com.campus.system.vo.AiAssistantHistoryMessageVO;
import com.campus.system.vo.AiAssistantMessageVO;
import com.campus.system.vo.AiAssistantSessionVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

// 统一 AI 助手业务接口
public interface AiAssistantService {

    List<AiAssistantSessionVO> listSessions();

    Long createSession();

    List<AiAssistantHistoryMessageVO> listMessages(Long sessionId);

    void renameSession(Long sessionId, String title);

    void deleteSession(Long sessionId);

    AiAssistantMessageVO chat(AiAssistantChatDTO body);

    void confirmExport(AiExportConfirmDTO body, HttpServletResponse response);
}
