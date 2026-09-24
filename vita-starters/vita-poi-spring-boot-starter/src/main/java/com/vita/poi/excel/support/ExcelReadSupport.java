package com.vita.poi.excel.support;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.metadata.Cell;
import com.alibaba.excel.metadata.data.CellData;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import com.vita.poi.excel.model.request.ExcelImportRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 读 Excel 支持类。
 */
public class ExcelReadSupport {

    public <T> void applyWorkbookDefaults(ExcelReaderBuilder builder,
                                          ExcelImportRequest<T> request,
                                          PoiExcelProperty property) {
        builder.autoCloseStream(property.isAutoCloseStream());
        builder.ignoreEmptyRow(property.isIgnoreEmptyRow());
        builder.autoTrim(property.isTrimCellValue());
        if (!CollectionUtils.isEmpty(request.getConverters())) {
            request.getConverters().forEach(builder::registerConverter);
        }
        if (request.getReadWorkbookCustomizer() != null) {
            request.getReadWorkbookCustomizer().accept(builder);
        }
    }

    public <T> ExcelReaderSheetBuilder createSheetBuilder(ExcelReaderBuilder builder,
                                                          ExcelImportRequest<T> request,
                                                          PoiExcelProperty property) {
        ExcelReaderSheetBuilder sheetBuilder;
        if (request.getSheetNo() != null) {
            sheetBuilder = builder.sheet(request.getSheetNo());
        } else if (StringUtils.hasText(request.getSheetName())) {
            sheetBuilder = builder.sheet(request.getSheetName());
        } else {
            sheetBuilder = builder.sheet();
        }
        sheetBuilder.headRowNumber(resolveHeadRowNumber(request, property));
        if (request.getReadSheetCustomizer() != null) {
            request.getReadSheetCustomizer().accept(sheetBuilder);
        }
        return sheetBuilder;
    }

    public int resolveHeadRowNumber(ExcelImportRequest<?> request, PoiExcelProperty property) {
        return request.getHeadRowNumber() != null ? request.getHeadRowNumber() : property.getDefaultHeadRowNumber();
    }

    public int resolveBatchSize(ExcelImportRequest<?> request, PoiExcelProperty property) {
        return request.getBatchSize() != null ? request.getBatchSize() : property.getDefaultImportBatchSize();
    }

    public Integer resolveRowIndex(AnalysisContext context) {
        if (context == null || context.readRowHolder() == null || context.readRowHolder().getRowIndex() == null) {
            return null;
        }
        return context.readRowHolder().getRowIndex() + 1;
    }

    public Integer resolveRowIndex(Exception exception, AnalysisContext context) {
        if (exception instanceof ExcelDataConvertException convertException && convertException.getRowIndex() != null) {
            return convertException.getRowIndex() + 1;
        }
        return resolveRowIndex(context);
    }

    public Integer resolveSheetNo(AnalysisContext context) {
        if (context == null || context.readSheetHolder() == null) {
            return null;
        }
        return context.readSheetHolder().getSheetNo();
    }

    public String resolveSheetName(AnalysisContext context) {
        if (context == null || context.readSheetHolder() == null) {
            return null;
        }
        return context.readSheetHolder().getSheetName();
    }

    public Map<Integer, String> extractRawData(AnalysisContext context, boolean trimCellValue) {
        Map<Integer, String> rawData = new LinkedHashMap<>();
        if (context == null || context.readRowHolder() == null || context.readRowHolder().getCellMap() == null) {
            return rawData;
        }
        Map<Integer, Cell> sorted = new TreeMap<>(context.readRowHolder().getCellMap());
        for (Map.Entry<Integer, Cell> entry : sorted.entrySet()) {
            rawData.put(entry.getKey(), normalizeCellText(entry.getValue(), trimCellValue));
        }
        return rawData;
    }

    public String resolveFieldName(Exception exception) {
        if (exception instanceof ExcelDataConvertException convertException
                && convertException.getExcelContentProperty() != null
                && convertException.getExcelContentProperty().getField() != null) {
            return convertException.getExcelContentProperty().getField().getName();
        }
        return null;
    }

    public String resolveErrorMessage(Exception exception) {
        if (exception == null) {
            return "Excel operation failed";
        }
        if (StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }
        if (exception.getCause() != null && StringUtils.hasText(exception.getCause().getMessage())) {
            return exception.getCause().getMessage();
        }
        return exception.getClass().getSimpleName();
    }

    private String normalizeCellText(Cell cell, boolean trimCellValue) {
        String value = null;
        if (cell instanceof CellData<?> cellData) {
            value = switch (cellData.getType()) {
                case NUMBER -> {
                    BigDecimal numberValue = cellData.getNumberValue();
                    yield numberValue == null ? null : numberValue.stripTrailingZeros().toPlainString();
                }
                case BOOLEAN -> cellData.getBooleanValue() == null ? null : String.valueOf(cellData.getBooleanValue());
                case EMPTY -> "";
                default -> StringUtils.hasText(cellData.getStringValue())
                        ? cellData.getStringValue()
                        : (cellData.getData() == null ? null : String.valueOf(cellData.getData()));
            };
        }
        if (value == null) {
            return null;
        }
        return trimCellValue ? value.trim() : value;
    }
}
