package com.campus.system.service.asset;

import com.campus.system.common.exception.BusinessException;
import com.campus.system.dto.RecognizedAssetRow;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

// Excel 资产清单识别服务
@Component
public class AssetExcelRecognitionService {

    private static final int MAX_ROWS = 500;

    private static final Map<String, String> COLUMN_ALIASES = Map.ofEntries(Map.entry("资产名称", "assetName"),
            Map.entry("名称", "assetName"), Map.entry("设备名称", "assetName"), Map.entry("资产分类", "categoryText"),
            Map.entry("分类", "categoryText"), Map.entry("类别", "categoryText"), Map.entry("购入日期", "purchaseDate"),
            Map.entry("购买日期", "purchaseDate"), Map.entry("采购日期", "purchaseDate"), Map.entry("位置", "locationText"),
            Map.entry("存放位置", "locationText"), Map.entry("安装位置", "locationText"), Map.entry("地点", "locationText")
    );

    // 识别工作表中的资产行，空白行自动忽略
    public List<RecognizedAssetRow> recognize(InputStream input) {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw BusinessException.badRequest("Excel 文件没有可用工作表");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw BusinessException.badRequest("Excel 文件没有表头行");
            }
            Map<Integer, String> columnMapping = mapHeader(headerRow);
            if (columnMapping.isEmpty()) {
                throw BusinessException.badRequest("未识别到可导入的资产列，请检查表头");
            }
            List<RecognizedAssetRow> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            int lastRow = sheet.getLastRowNum();
            for (int i = sheet.getFirstRowNum() + 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, columnMapping, formatter)) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    throw BusinessException.badRequest("单次最多识别 500 行资产数据，请拆分文件后重试");
                }
                RecognizedAssetRow recognized = parseRow(row, i + 1, columnMapping, formatter);
                rows.add(recognized);
            }
            if (rows.isEmpty()) {
                throw BusinessException.badRequest("未识别到有效资产数据");
            }
            return rows;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("无法读取 Excel 文件，请确认格式为 .xlsx");
        }
    }

    private Map<Integer, String> mapHeader(Row headerRow) {
        Map<Integer, String> mapping = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            String field = COLUMN_ALIASES.get(header);
            if (field != null) {
                mapping.put(cell.getColumnIndex(), field);
            }
        }
        return mapping;
    }

    private RecognizedAssetRow parseRow(Row row, int rowNumber, Map<Integer, String> columnMapping, DataFormatter formatter) {
        RecognizedAssetRow recognized = new RecognizedAssetRow();
        recognized.setRowNumber(rowNumber);
        for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
            Cell cell = row.getCell(entry.getKey());
            String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
            switch (entry.getValue()) {
                case "assetName" -> recognized.setAssetName(value);
                case "categoryText" -> recognized.setCategoryText(value);
                case "locationText" -> recognized.setLocationText(value);
                case "purchaseDate" -> recognized.setPurchaseDate(parseDate(cell, value));
                default -> {}
            }
        }
        return recognized;
    }

    private LocalDate parseDate(Cell cell, String text) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        }
        if (text == null || text.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/M/d"),
                DateTimeFormatter.ofPattern("yyyy年M月d日")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {

            }
        }
        return null;
    }

    private boolean isBlankRow(Row row, Map<Integer, String> columnMapping, DataFormatter formatter) {
        for (Integer columnIndex : columnMapping.keySet()) {
            Cell cell = row.getCell(columnIndex);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String normalized = header.trim().replaceAll("\\s+", "");
        for (String alias : COLUMN_ALIASES.keySet()) {
            if (alias.equals(normalized)) {
                return alias;
            }
        }
        return normalized;
    }
}
