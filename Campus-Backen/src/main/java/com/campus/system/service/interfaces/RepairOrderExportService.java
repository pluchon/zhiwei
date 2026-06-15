package com.campus.system.service.interfaces;

import com.campus.system.dto.RepairOrderQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

// 工单导出业务接口
public interface RepairOrderExportService {

    void export(RepairOrderQueryDTO query, HttpServletResponse response);
}
