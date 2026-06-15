package com.campus.system.controller;

import com.campus.system.service.interfaces.SsePushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// SSE 实时通知接口
@RestController
@RequestMapping("/notifications")
public class SseController {

    @Autowired
    private SsePushService ssePushService;

    /**
     * 建立当前登录用户的 SSE 通知连接
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return ssePushService.connect();
    }
}
