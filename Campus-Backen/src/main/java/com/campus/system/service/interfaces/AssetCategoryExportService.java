package com.campus.system.service.interfaces;

import com.campus.system.dto.AssetCategoryQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

// 资产分类导出
public interface AssetCategoryExportService {

    void export(AssetCategoryQueryDTO query, HttpServletResponse response);
}
