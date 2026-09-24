package com.vita.poi.excel.support;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.vita.poi.excel.model.request.ExcelFillRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 模板填充支持类。
 */
public class ExcelFillSupport {

    public ExcelWriterBuilder createWriterBuilder(ExcelFillRequest request, PoiExcelProperty property) {
        ExcelWriterBuilder builder = EasyExcel.write(request.getOutputStream())
                .withTemplate(request.getTemplateInputStream());
        builder.autoCloseStream(property.isAutoCloseStream());
        builder.inMemory(property.isInMemory());
        builder.useDefaultStyle(property.isUseDefaultStyle());
        if (request.getWriteCustomizer() != null) {
            request.getWriteCustomizer().accept(builder);
        }
        return builder;
    }

    public ExcelWriterSheetBuilder createSheetBuilder(ExcelWriter writer, ExcelFillRequest request, PoiExcelProperty property) {
        ExcelWriterSheetBuilder sheetBuilder = new ExcelWriterSheetBuilder(writer);
        if (request.getSheetNo() != null) {
            sheetBuilder.sheetNo(request.getSheetNo());
        } else if (StringUtils.hasText(request.getSheetName())) {
            sheetBuilder.sheetName(request.getSheetName().trim());
        } else {
            sheetBuilder.sheetName(property.getDefaultSheetName());
        }
        sheetBuilder.useDefaultStyle(property.isUseDefaultStyle());
        if (!CollectionUtils.isEmpty(request.getWriteHandlers())) {
            request.getWriteHandlers().forEach(sheetBuilder::registerWriteHandler);
        }
        return sheetBuilder;
    }
}
