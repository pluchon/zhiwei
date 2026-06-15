package com.campus.system.service.interfaces;

import com.campus.system.dto.UserQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

// 用户导出服务
public interface UserExportService {

    void export(UserQueryDTO query, HttpServletResponse response);
}
