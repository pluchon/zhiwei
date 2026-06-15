package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.RepairerSuggestionHandleDTO;
import com.campus.system.dto.RepairerSuggestionQueryDTO;
import com.campus.system.dto.RepairerSuggestionSubmitDTO;
import com.campus.system.service.interfaces.RepairerSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 维修师傅建议接口
@RestController
@RequestMapping("/repair/suggestions")
public class RepairerSuggestionController {

    @Autowired
    private RepairerSuggestionService service;

    /**
     * 维修师傅查询本人建议列表
     */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> myList(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, RepairerSuggestionQueryDTO query) {
        return ApiResponse.ok(service.myList(pageNum, pageSize, query));
    }

    /**
     * 管理员查询全部建议列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> adminList(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, RepairerSuggestionQueryDTO query) {
        return ApiResponse.ok(service.adminList(pageNum, pageSize, query));
    }

    /**
     * 查询建议详情
     */
    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    /**
     * 提交前检测相似建议
     */
    @PostMapping("/similarity-check")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> checkSimilarity(@RequestBody RepairerSuggestionSubmitDTO body,
            @RequestParam(required = false) Long excludeSuggestionId) {
        return ApiResponse.ok(service.checkSimilarity(body, excludeSuggestionId));
    }

    /**
     * 维修师傅提交建议
     */
    @PostMapping
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> submit(@RequestBody RepairerSuggestionSubmitDTO body) {
        return ApiResponse.ok(service.submit(body));
    }

    /**
     * 维修师傅编辑并重新提交建议
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody RepairerSuggestionSubmitDTO body) {
        return ApiResponse.ok(service.update(id, body));
    }

    /**
     * 维修师傅撤回待处理建议
     */
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> withdraw(@PathVariable Long id) {
        service.withdraw(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员处理建议
     */
    @PostMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> handle(@PathVariable Long id, @RequestBody RepairerSuggestionHandleDTO body) {
        service.handle(id, body);
        return ApiResponse.ok(null);
    }
}
