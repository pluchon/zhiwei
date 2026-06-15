package com.campus.system.controller;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.dto.RepairerAvailabilityUpdateDTO;
import com.campus.system.service.interfaces.RepairerAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 维修师傅接单状态接口
@RestController
public class RepairerAvailabilityController {

    @Autowired
    private RepairerAvailabilityService service;

    /**
     * 维修师傅查询本人接单状态
     */
    @GetMapping("/repair/repairer/availability")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> mine() {
        return ApiResponse.ok(service.mine());
    }

    /**
     * 维修师傅更新本人接单状态
     */
    @PutMapping("/repair/repairer/availability")
    @PreAuthorize("hasRole('REPAIRER')")
    public ApiResponse<?> updateMine(@RequestBody RepairerAvailabilityUpdateDTO body) {
        return ApiResponse.ok(service.updateMine(body));
    }

    /**
     * 管理员只读查询维修师傅接单状态列表
     */
    @GetMapping("/admin/repairers/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> listForAdmin(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String acceptingState) {
        return ApiResponse.ok(service.listForAdmin(pageNum, pageSize, acceptingState));
    }
}
