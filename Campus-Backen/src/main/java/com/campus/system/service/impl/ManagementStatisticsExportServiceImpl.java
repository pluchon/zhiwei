package com.campus.system.service.impl;

import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.dto.StatisticsQueryDTO;
import com.campus.system.entity.ManagementStatisticsExportLog;
import com.campus.system.mapper.ManagementStatisticsExportLogMapper;
import com.campus.system.service.interfaces.ManagementStatisticsExportService;
import com.campus.system.service.interfaces.ManagementStatisticsService;
import com.campus.system.vo.AssetCategoryRepairStatVO;
import com.campus.system.vo.AssetRepairRiskItemVO;
import com.campus.system.vo.ManagementStatisticsVO;
import com.campus.system.vo.RepairEfficiencyStatVO;
import com.campus.system.vo.StatisticsDistributionItemVO;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 管理统计导出实现
@Service
public class ManagementStatisticsExportServiceImpl implements ManagementStatisticsExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ManagementStatisticsService statisticsService;

    @Autowired
    private ManagementStatisticsExportLogMapper exportLogs;

    @Autowired
    private BusinessClock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void export(StatisticsQueryDTO query, HttpServletResponse response) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可导出统计");
        }
        StatisticsQueryDTO safeQuery = query == null ? new StatisticsQueryDTO() : query;
        String rangeType = safeQuery.getRangeType() == null ? "LAST_30_DAYS" : safeQuery.getRangeType();
        String fileName = "management_statistics_" + clock.now().format(FILE_TIME) + ".xlsx";
        try {
            ManagementStatisticsVO data = statisticsService.overview(safeQuery);
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                writeEfficiencySheet(workbook.createSheet("核心指标"), data.getRepairEfficiency());
                writeTrendSheet(workbook.createSheet("未完成工单趋势"), data.getUnfinishedOrderTrend());
                writeAssetSheet(workbook.createSheet("高频维修资产"), data.getTopRepairedAssets());
                writeCategorySheet(workbook.createSheet("分类报修"), data.getAssetCategoryRepairs());
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                workbook.write(response.getOutputStream());
            }
            writeExportLog(SecurityUtils.current().userId(), rangeType, "SUCCESS", fileName, null);
        } catch (BusinessException ex) {
            writeExportLog(SecurityUtils.current().userId(), rangeType, "FAILED", null, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            writeExportLog(SecurityUtils.current().userId(), rangeType, "FAILED", null, ex.getMessage());
            throw BusinessException.conflict("导出失败，请稍后重试");
        }
    }

    private void writeEfficiencySheet(Sheet sheet, RepairEfficiencyStatVO stat) {
        if (stat == null) {
            stat = new RepairEfficiencyStatVO();
        }
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("指标");
        header.createCell(1).setCellValue("数值");
        String[][] rows = {{"已完成工单数", String.valueOf(stat.getCompletedCount())},
                {"超过七天完成数", String.valueOf(stat.getOverSevenDaysCount())}, {"当前未完成工单数", String.valueOf(stat.getUnfinishedCount())},
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
    }

    private void writeAssetSheet(Sheet sheet, List<AssetRepairRiskItemVO> items) {
        Row header = sheet.createRow(0);
        String[] headers = {"资产编号", "资产名称", "维修次数", "购入日期", "已购入年数", "已购入月数"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        if (items == null) {
            return;
        }
        int rowIndex = 1;
        for (AssetRepairRiskItemVO item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(nullToEmpty(item.getAssetNo()));
            row.createCell(1).setCellValue(nullToEmpty(item.getAssetName()));
            row.createCell(2).setCellValue(item.getRepairCount() == null ? 0 : item.getRepairCount());
            row.createCell(3).setCellValue(item.getPurchaseDate() == null ? "" : item.getPurchaseDate().toString());
            row.createCell(4).setCellValue(item.getPurchaseYears() == null ? 0 : item.getPurchaseYears());
            row.createCell(5).setCellValue(item.getPurchaseMonths() == null ? 0 : item.getPurchaseMonths());
        }
    }

    private void writeCategorySheet(Sheet sheet, List<AssetCategoryRepairStatVO> items) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("资产分类");
        header.createCell(1).setCellValue("报修数量");
        if (items == null) {
            return;
        }
        int rowIndex = 1;
        for (AssetCategoryRepairStatVO item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(nullToEmpty(item.getCategoryName()));
            row.createCell(1).setCellValue(item.getRepairCount() == null ? 0 : item.getRepairCount());
        }
    }

    private void writeTrendSheet(Sheet sheet, List<StatisticsDistributionItemVO> items) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("未完成工单数");
        if (items == null) {
            return;
        }
        int rowIndex = 1;
        for (StatisticsDistributionItemVO item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(nullToEmpty(item.getName()));
            row.createCell(1).setCellValue(item.getCount());
        }
    }

    private void writeExportLog(Long operatorId, String rangeType, String status, String fileName, String failureReason) {
        ManagementStatisticsExportLog log = new ManagementStatisticsExportLog();
        log.setOperatorId(operatorId);
        log.setRangeType(rangeType);
        log.setResultStatus(status);
        log.setFileName(fileName);
        log.setFailureReason(failureReason);
        exportLogs.insert(log);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
