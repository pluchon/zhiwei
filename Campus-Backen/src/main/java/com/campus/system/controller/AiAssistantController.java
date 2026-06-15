package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.AiAssistantChatDTO;
import com.campus.system.dto.AiAssistantSessionRenameDTO;
import com.campus.system.dto.AiExportConfirmDTO;
import com.campus.system.service.interfaces.AiAssistantService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 统一 AI 助手接口（登录即可访问，角色校验在 Service 层）
@RestController
@RequestMapping("/ai/assistant")
public class AiAssistantController {

    @Autowired
    private AiAssistantService service;

    /**
     * 会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<?>> listSessions() {
        return ApiResponse.ok(service.listSessions());
    }

    /**
     * 新建会话
     */
    @PostMapping("/sessions")
    public ApiResponse<Long> createSession() {
        return ApiResponse.ok(service.createSession());
    }

    /**
     * 会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<?>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.ok(service.listMessages(sessionId));
    }

    /**
     * 重命名会话
     */
    @PutMapping("/sessions/{sessionId}")
    public ApiResponse<Void> renameSession(@PathVariable Long sessionId, @RequestBody AiAssistantSessionRenameDTO body) {
        service.renameSession(sessionId, body == null ? null : body.getTitle());
        return ApiResponse.ok(null);
    }

    /**
     * 重命名会话（POST 兼容）
     */
    @PostMapping("/sessions/{sessionId}/rename")
    public ApiResponse<Void> renameSessionPost(@PathVariable Long sessionId, @RequestBody AiAssistantSessionRenameDTO body) {
        service.renameSession(sessionId, body == null ? null : body.getTitle());
        return ApiResponse.ok(null);
    }

    /**
     * 逻辑删除会话
     */
    @PostMapping("/sessions/{sessionId}/delete")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        service.deleteSession(sessionId);
        return ApiResponse.ok(null);
    }

    /**
     * AI 助手多轮对话
     */
    @PostMapping("/chat")
    public ApiResponse<?> chat(@RequestBody AiAssistantChatDTO body) {
        return ApiResponse.ok(service.chat(body));
    }

    /**
     * 确认导出预览并执行导出
     */
    @PostMapping("/export/confirm")
    public void confirmExport(@RequestBody AiExportConfirmDTO body, HttpServletResponse response) {
        service.confirmExport(body, response);
    }
}
