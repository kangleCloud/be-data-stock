package com.vita.poi.excel.support;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.vita.poi.excel.model.request.ExcelExportRequest;
import com.vita.poi.excel.model.request.ExcelPagedExportRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 写 Excel 支持类。
 */
public class ExcelWriteSupport {

    public <T> ExcelWriterBuilder createWriterBuilder(ExcelExportRequest<T> request, PoiExcelProperty property) {
        ExcelWriterBuilder builder = EasyExcel.write(request.getOutputStream(), request.getHeadClass());
        applyWorkbookDefaults(builder, property, request.getConverters(), request.getWriteWorkbookCustomizer());
        return builder;
    }

    public <T> ExcelWriterBuilder createWriterBuilder(ExcelPagedExportRequest<T> request, PoiExcelProperty property) {
        ExcelWriterBuilder builder = EasyExcel.write(request.getOutputStream(), request.getHeadClass());
        applyWorkbookDefaults(builder, property, request.getConverters(), request.getWriteWorkbookCustomizer());
        return builder;
    }

    public <T> ExcelWriterSheetBuilder createSheetBuilder(ExcelWriterBuilder builder,
                                                          ExcelExportRequest<T> request,
                                                          PoiExcelProperty property) {
        ExcelWriterSheetBuilder sheetBuilder = builder.sheet(resolveSheetName(request.getSheetName(), property));
        applySheetDefaults(
                sheetBuilder,
                property,
                request.getNeedHead(),
                request.getRelativeHeadRowIndex(),
                request.getIncludeColumnIndexes(),
                request.getExcludeColumnIndexes(),
                request.getIncludeColumnFieldNames(),
                request.getExcludeColumnFieldNames(),
                request.getWriteHandlers(),
                request.getWriteSheetCustomizer()
        );
        return sheetBuilder;
    }

    public <T> ExcelWriterSheetBuilder createSheetBuilder(ExcelWriter writer,
                                                          ExcelPagedExportRequest<T> request,
                                                          PoiExcelProperty property) {
        ExcelWriterSheetBuilder sheetBuilder = new ExcelWriterSheetBuilder(writer);
        sheetBuilder.sheetName(resolveSheetName(request.getSheetName(), property));
        applySheetDefaults(
                sheetBuilder,
                property,
                request.getNeedHead(),
                request.getRelativeHeadRowIndex(),
                request.getIncludeColumnIndexes(),
                request.getExcludeColumnIndexes(),
                request.getIncludeColumnFieldNames(),
                request.getExcludeColumnFieldNames(),
                request.getWriteHandlers(),
                request.getWriteSheetCustomizer()
        );
        return sheetBuilder;
    }

    public int resolvePageSize(ExcelPagedExportRequest<?> request, PoiExcelProperty property) {
        return request.getPageSize() != null ? request.getPageSize() : property.getDefaultExportPageSize();
    }

    public String resolveSheetName(String sheetName, PoiExcelProperty property) {
        if (StringUtils.hasText(sheetName)) {
            return sheetName.trim();
        }
        return property.getDefaultSheetName();
    }

    private void applyWorkbookDefaults(ExcelWriterBuilder builder,
                                       PoiExcelProperty property,
                                       java.util.List<Converter<?>> converters,
                                       java.util.function.Consumer<ExcelWriterBuilder> customizer) {
        builder.autoCloseStream(property.isAutoCloseStream());
        builder.inMemory(property.isInMemory());
        builder.useDefaultStyle(property.isUseDefaultStyle());
        if (!CollectionUtils.isEmpty(converters)) {
            converters.forEach(builder::registerConverter);
        }
        if (customizer != null) {
            customizer.accept(builder);
        }
    }

    private void applySheetDefaults(ExcelWriterSheetBuilder sheetBuilder,
                                    PoiExcelProperty property,
                                    Boolean needHead,
                                    Integer relativeHeadRowIndex,
                                    java.util.Collection<Integer> includeColumnIndexes,
                                    java.util.Collection<Integer> excludeColumnIndexes,
                                    java.util.Collection<String> includeColumnFieldNames,
                                    java.util.Collection<String> excludeColumnFieldNames,
                                    java.util.List<WriteHandler> writeHandlers,
                                    java.util.function.Consumer<ExcelWriterSheetBuilder> customizer) {
        sheetBuilder.useDefaultStyle(property.isUseDefaultStyle());
        if (needHead != null) {
            sheetBuilder.needHead(needHead);
        }
        if (relativeHeadRowIndex != null) {
            sheetBuilder.relativeHeadRowIndex(relativeHeadRowIndex);
        }
        if (!CollectionUtils.isEmpty(includeColumnIndexes)) {
            sheetBuilder.includeColumnIndexes(includeColumnIndexes);
        }
        if (!CollectionUtils.isEmpty(excludeColumnIndexes)) {
            sheetBuilder.excludeColumnIndexes(excludeColumnIndexes);
        }
        if (!CollectionUtils.isEmpty(includeColumnFieldNames)) {
            sheetBuilder.includeColumnFieldNames(includeColumnFieldNames);
        }
        if (!CollectionUtils.isEmpty(excludeColumnFieldNames)) {
            sheetBuilder.excludeColumnFieldNames(excludeColumnFieldNames);
        }
        if (!CollectionUtils.isEmpty(writeHandlers)) {
            writeHandlers.forEach(sheetBuilder::registerWriteHandler);
        }
        if (customizer != null) {
            customizer.accept(sheetBuilder);
        }
    }
}
