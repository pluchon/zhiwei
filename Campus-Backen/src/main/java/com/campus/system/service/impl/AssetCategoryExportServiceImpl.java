package com.campus.system.service.impl;

import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.dto.AssetCategoryQueryDTO;
import com.campus.system.service.interfaces.AssetCategoryExportService;
import com.campus.system.service.interfaces.AssetCategoryService;
import com.campus.system.vo.AssetCategoryVO;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 资产分类导出实现
@Service
public class AssetCategoryExportServiceImpl implements AssetCategoryExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private AssetCategoryService categoryService;

    @Autowired
    private BusinessClock clock;

    @Override
    public void export(AssetCategoryQueryDTO query, HttpServletResponse response) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可导出资产分类");
        }
        AssetCategoryQueryDTO safeQuery = query == null ? new AssetCategoryQueryDTO() : query;
        List<AssetCategoryVO> rows;
        if (safeQuery.getAssetCategoryIds() != null && !safeQuery.getAssetCategoryIds().isBlank()) {
            List<Long> ids = Arrays.stream(safeQuery.getAssetCategoryIds().split(",")).map(String::trim).filter(part -> !part.isEmpty()).map(Long::valueOf).distinct().toList();
            rows = categoryService.listForExportByIds(ids);
        } else {
            rows = categoryService.listForExport(safeQuery);
        }
        String fileName = "asset_categories_" + clock.now().format(FILE_TIME) + ".xlsx";
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资产分类");
            writeHeader(sheet.createRow(0));
            int rowIndex = 1;
            for (AssetCategoryVO row : rows) {
                writeRow(sheet.createRow(rowIndex++), row);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.conflict("导出失败，请稍后重试");
        }
    }

    private void writeHeader(Row row) {
        String[] headers = {"ID", "分类名称", "状态", "创建时间", "更新时间"};
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }

    private void writeRow(Row row, AssetCategoryVO category) {
        row.createCell(0).setCellValue(category.getAssetCategoryId() == null ? "" : String.valueOf(category.getAssetCategoryId()));
        row.createCell(1).setCellValue(nullToEmpty(category.getCategoryName()));
        row.createCell(2).setCellValue(statusLabel(category.getStatus()));
        row.createCell(3).setCellValue(category.getCreateTime() != null ? category.getCreateTime().toString() : "");
        row.createCell(4).setCellValue(category.getUpdateTime() != null ? category.getUpdateTime().toString() : "");
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 0 ? "启用" : "停用";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
