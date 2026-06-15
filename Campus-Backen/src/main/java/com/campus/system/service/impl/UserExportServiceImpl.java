package com.campus.system.service.impl;

import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.time.BusinessClock;
import com.campus.system.dto.UserQueryDTO;
import com.campus.system.entity.SysRole;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.service.interfaces.AdminService;
import com.campus.system.service.interfaces.UserExportService;
import com.campus.system.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

// 用户导出实现
@Service
public class UserExportServiceImpl implements UserExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private AdminService adminService;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private BusinessClock clock;

    @Override
    public void export(UserQueryDTO query, HttpServletResponse response) {
        if (!"ADMIN".equals(SecurityUtils.current().roleCode())) {
            throw BusinessException.forbidden("仅管理员可导出用户");
        }
        UserQueryDTO safeQuery = query == null ? new UserQueryDTO() : query;
        List<UserVO> rows;
        if (safeQuery.getUserIds() != null && !safeQuery.getUserIds().isBlank()) {
            List<Long> userIds = Arrays.stream(safeQuery.getUserIds().split(","))
                    .map(String::trim).filter(part -> !part.isEmpty()).map(Long::valueOf).distinct().toList();
            rows = adminService.listUsersForExportByIds(userIds);
        } else {
            rows = adminService.listUsersForExport(safeQuery);
        }
        String fileName = "users_" + clock.now().format(FILE_TIME) + ".xlsx";
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户");
            writeHeader(sheet.createRow(0));
            Map<Long, String> roleNames = loadRoleNames(rows);
            int rowIndex = 1;
            for (UserVO user : rows) {
                writeRow(sheet.createRow(rowIndex++), user, roleNames);
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
        String[] headers = {"ID", "账号", "姓名", "昵称", "角色", "手机号", "激活状态", "账号状态", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }

    private void writeRow(Row row, UserVO user, Map<Long, String> roleNames) {
        row.createCell(0).setCellValue(user.getUserId() == null ? "" : String.valueOf(user.getUserId()));
        row.createCell(1).setCellValue(nullToEmpty(user.getUserNo()));
        row.createCell(2).setCellValue(nullToEmpty(user.getRealName()));
        row.createCell(3).setCellValue(nullToEmpty(user.getNickName()));
        row.createCell(4).setCellValue(roleNames.getOrDefault(user.getRoleId(), ""));
        row.createCell(5).setCellValue(nullToEmpty(user.getPhoneNumber()));
        row.createCell(6).setCellValue(activationLabel(user.getActivationStatus()));
        row.createCell(7).setCellValue(accountLabel(user.getAccountStatus()));
        row.createCell(8).setCellValue(user.getCreateTime() != null ? user.getCreateTime().toString() : "");
    }

    private Map<Long, String> loadRoleNames(List<UserVO> rows) {
        Map<Long, String> map = new HashMap<>();
        for (UserVO row : rows) {
            if (row.getRoleId() != null && !map.containsKey(row.getRoleId())) {
                SysRole role = roles.selectById(row.getRoleId());
                map.put(row.getRoleId(), role != null ? role.getRoleName() : "");
            }
        }
        return map;
    }

    private String activationLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "已激活" : "未激活";
    }

    private String accountLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 0 ? "正常" : "停用";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
