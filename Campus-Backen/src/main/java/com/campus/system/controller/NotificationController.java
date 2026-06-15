package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.NotificationBatchReadDTO;
import com.campus.system.service.interfaces.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 站内通知
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    /**
     * 分页查询当前用户的站内通知列表
    */
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer isRead, @RequestParam(required = false) Integer notificationType) {
        return ApiResponse.ok(service.list(pageNum, pageSize, isRead, notificationType));
    }

    /**
     * 获取当前用户的未读通知数量及状态
    */
    @GetMapping("/unread-state")
    public ApiResponse<?> unreadState() {
        return ApiResponse.ok(service.unreadState());
    }

    /**
     * 标记单条通知为已读
    */
    @PutMapping("/{id}/read")
    public ApiResponse<?> read(@PathVariable Long id) {
        service.markRead(id);
        return ApiResponse.ok(null);
    }

    /**
     * 批量标记指定的通知为已读
    */
    @PostMapping("/read-batch")
    public ApiResponse<?> readBatch(@RequestBody NotificationBatchReadDTO body) {
        service.markReadBatch(body);
        return ApiResponse.ok(null);
    }

    /**
     * 标记当前用户的所有未读通知为已读
    */
    @PostMapping("/read-all")
    public ApiResponse<?> readAll() {
        service.markAllRead();
        return ApiResponse.ok(null);
    }
}
