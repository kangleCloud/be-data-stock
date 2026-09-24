package com.vita.poi.excel.service.impl;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.vita.core.exception.ServiceException;
import com.vita.poi.excel.model.request.ExcelFillRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.service.ExcelFillService;
import com.vita.poi.excel.support.ExcelFillSupport;
import com.vita.poi.excel.support.ExcelRequestValidator;

/**
 * 默认 Excel 模板填充服务。
 */
public class DefaultExcelFillService implements ExcelFillService {

    private final PoiExcelProperty property;
    private final ExcelRequestValidator requestValidator;
    private final ExcelFillSupport fillSupport;

    public DefaultExcelFillService(PoiExcelProperty property,
                                   ExcelRequestValidator requestValidator,
                                   ExcelFillSupport fillSupport) {
        this.property = property;
        this.property.normalizeAndValidate();
        this.requestValidator = requestValidator;
        this.fillSupport = fillSupport;
    }

    @Override
    public void fill(ExcelFillRequest request) {
        requestValidator.validateFillRequest(request, property);
        ExcelWriter writer = null;
        try {
            ExcelWriterBuilder writerBuilder = fillSupport.createWriterBuilder(request, property);
            writer = writerBuilder.build();
            ExcelWriterSheetBuilder sheetBuilder = fillSupport.createSheetBuilder(writer, request, property);
            WriteSheet writeSheet = sheetBuilder.build();
            if (request.getObjectData() != null) {
                writer.fill(request.getObjectData(), writeSheet);
            }
            if (request.getListData() != null) {
                if (request.getFillConfig() != null) {
                    writer.fill(request.getListData(), request.getFillConfig(), writeSheet);
                } else {
                    writer.fill(request.getListData(), writeSheet);
                }
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw requestValidator.businessException("Excel fill failed: " + exception.getMessage());
        } finally {
            if (writer != null) {
                writer.finish();
            }
        }
    }
}
