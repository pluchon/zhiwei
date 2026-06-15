package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.system.common.enums.RepairStatus;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.dto.RepairOrderQueryDTO;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.RepairOrder;
import com.campus.system.entity.RepairOrderExportLog;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.RepairOrderExportLogMapper;
import com.campus.system.mapper.RepairOrderMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.RepairOrderExportService;
import com.campus.system.service.interfaces.RepairOrderService;
import com.campus.system.vo.RepairOrderVO;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 工单同步导出实现
@Service
public class RepairOrderExportServiceImpl implements RepairOrderExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private RepairOrderMapper orderMapper;

    @Autowired
    private RepairOrderExportLogMapper exportLogs;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private BusinessClock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void export(RepairOrderQueryDTO query, HttpServletResponse response) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可导出工单");
        }
        RepairOrderQueryDTO safeQuery = query == null ? new RepairOrderQueryDTO() : query;
        List<RepairOrderVO> rows;
        if (safeQuery.getOrderIds() != null && !safeQuery.getOrderIds().isBlank()) {
            List<Long> orderIds = Arrays.stream(safeQuery.getOrderIds().split(",")).map(String::trim)
                    .filter(part -> !part.isEmpty()).map(Long::valueOf).distinct().toList();
            rows = repairOrderService.listForExportByIds(orderIds);
        } else {
            PageResult<RepairOrderVO> page = repairOrderService.search(1, 1001, safeQuery);
            if (page.total() > 1000) {
                writeExportLog(SecurityUtils.current().userId(), safeQuery, 0, "FAILED", null, "超过单次导出上限 1000 条");
                throw BusinessException.badRequest("导出数量超过 1000 条，请缩小筛选范围");
            }
            rows = page.records();
        }
        String fileName = "repair_orders_" + clock.now().format(FILE_TIME) + ".xlsx";
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("工单");
            writeHeader(sheet.createRow(0));
            Map<Long, String> categoryNames = loadCategoryNames(rows);
            Map<Long, SysUser> userMap = loadUsers(rows);
            int rowIndex = 1;
            for (RepairOrderVO order : rows) {
                writeRow(sheet.createRow(rowIndex++), order, categoryNames, userMap);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            markExported(rows);
            writeExportLog(SecurityUtils.current().userId(), safeQuery, rows.size(), "SUCCESS", fileName, null);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            writeExportLog(SecurityUtils.current().userId(), safeQuery, 0, "FAILED", null, ex.getMessage());
            throw BusinessException.conflict("导出失败，请稍后重试");
        }
    }

    private void writeHeader(Row row) {
        String[] headers = {"工单编号", "标题", "状态", "故障类型", "位置", "资产编号", "资产名称", "报修人账号", "报修人姓名", "维修师傅账号", "维修师傅姓名", "创建时间", "确认完成时间"};
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }

    private void writeRow(Row row, RepairOrderVO order, Map<Long, String> categoryNames, Map<Long, SysUser> userMap) {
        row.createCell(0).setCellValue(nullToEmpty(order.getOrderNo()));
        row.createCell(1).setCellValue(nullToEmpty(order.getTitle()));
        row.createCell(2).setCellValue(String.valueOf(order.getStatus()));
        row.createCell(3).setCellValue(categoryNames.getOrDefault(order.getCategoryId(), ""));
        row.createCell(4).setCellValue(buildLocation(order));
        row.createCell(5).setCellValue(nullToEmpty(order.getAssetNoSnapshot()));
        row.createCell(6).setCellValue(nullToEmpty(order.getAssetNameSnapshot()));
        SysUser reporter = userMap.get(order.getReporterId());
        row.createCell(7).setCellValue(reporter != null ? reporter.getUserNo() : "");
        row.createCell(8).setCellValue(reporter != null ? reporter.getRealName() : "");
        SysUser repairer = order.getCurrentRepairerId() != null ? userMap.get(order.getCurrentRepairerId()) : null;
        row.createCell(9).setCellValue(repairer != null ? repairer.getUserNo() : "");
        row.createCell(10).setCellValue(repairer != null ? repairer.getRealName() : "");
        row.createCell(11).setCellValue(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
        row.createCell(12).setCellValue(order.getCompletionTime() != null ? order.getCompletionTime().toString() : "");
    }

    private String buildLocation(RepairOrderVO order) {
        StringBuilder sb = new StringBuilder();
        if (order.getCampus() != null) {
            sb.append(order.getCampus());
        }
        if (order.getBuilding() != null) {
            sb.append(" ").append(order.getBuilding());
        }
        if (order.getLocationDetail() != null) {
            sb.append(" ").append(order.getLocationDetail());
        }
        return sb.toString().trim();
    }

    private Map<Long, String> loadCategoryNames(List<RepairOrderVO> rows) {
        Map<Long, String> map = new HashMap<>();
        for (RepairOrderVO row : rows) {
            if (row.getCategoryId() != null && !map.containsKey(row.getCategoryId())) {
                RepairCategory category = categories.selectById(row.getCategoryId());
                map.put(row.getCategoryId(), category != null ? category.getCategoryName() : "");
            }
        }
        return map;
    }

    private Map<Long, SysUser> loadUsers(List<RepairOrderVO> rows) {
        Map<Long, SysUser> map = new HashMap<>();
        for (RepairOrderVO row : rows) {
            if (row.getReporterId() != null && !map.containsKey(row.getReporterId())) {
                map.put(row.getReporterId(), users.selectById(row.getReporterId()));
            }
            if (row.getCurrentRepairerId() != null && !map.containsKey(row.getCurrentRepairerId())) {
                map.put(row.getCurrentRepairerId(), users.selectById(row.getCurrentRepairerId()));
            }
        }
        return map;
    }

    private void markExported(List<RepairOrderVO> rows) {
        LocalDateTime now = clock.now();
        for (RepairOrderVO row : rows) {
            orderMapper.update(null, Wrappers.<RepairOrder>lambdaUpdate().set(RepairOrder::getExportedFlag, 1)
                    .set(RepairOrder::getFirstExportTime, now).eq(RepairOrder::getOrderId, row.getOrderId()).eq(RepairOrder::getExportedFlag, 0));
            orderMapper.update(null, Wrappers.<RepairOrder>lambdaUpdate().set(RepairOrder::getExportedFlag, 1)
                    .eq(RepairOrder::getOrderId, row.getOrderId()).eq(RepairOrder::getExportedFlag, 1));
        }
    }

    private void writeExportLog(Long operatorId, RepairOrderQueryDTO query, int count, String status, String fileName, String failureReason) {
        RepairOrderExportLog log = new RepairOrderExportLog();
        log.setOperatorId(operatorId);
        log.setFilterSnapshot(String.valueOf(query));
        log.setExportCount(count);
        log.setResultStatus(status);
        log.setFileName(fileName);
        log.setFailureReason(failureReason);
        exportLogs.insert(log);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
