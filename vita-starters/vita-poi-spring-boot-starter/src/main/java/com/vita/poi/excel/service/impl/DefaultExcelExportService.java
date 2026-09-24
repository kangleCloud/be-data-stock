package com.vita.poi.excel.service.impl;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.vita.core.exception.ServiceException;
import com.vita.poi.excel.model.request.ExcelExportRequest;
import com.vita.poi.excel.model.request.ExcelPagedExportRequest;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.service.ExcelExportService;
import com.vita.poi.excel.support.ExcelRequestValidator;
import com.vita.poi.excel.support.ExcelWriteSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 默认 Excel 导出服务。
 */
public class DefaultExcelExportService implements ExcelExportService {

    private final PoiExcelProperty property;
    private final ExcelRequestValidator requestValidator;
    private final ExcelWriteSupport writeSupport;

    public DefaultExcelExportService(PoiExcelProperty property,
                                     ExcelRequestValidator requestValidator,
                                     ExcelWriteSupport writeSupport) {
        this.property = property;
        this.property.normalizeAndValidate();
        this.requestValidator = requestValidator;
        this.writeSupport = writeSupport;
    }

    @Override
    public <T> void export(ExcelExportRequest<T> request) {
        requestValidator.validateExportRequest(request, property);
        try {
            ExcelWriterBuilder writerBuilder = writeSupport.createWriterBuilder(request, property);
            ExcelWriterSheetBuilder sheetBuilder = writeSupport.createSheetBuilder(writerBuilder, request, property);
            Collection<T> data = request.getData() == null ? Collections.emptyList() : request.getData();
            sheetBuilder.doWrite(data);
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw requestValidator.businessException("Excel export failed: " + exception.getMessage());
        }
    }

    @Override
    public <T> void exportByPage(ExcelPagedExportRequest<T> request) {
        requestValidator.validatePagedExportRequest(request, property);
        int pageSize = writeSupport.resolvePageSize(request, property);
        ExcelWriter writer = null;
        try {
            ExcelWriterBuilder writerBuilder = writeSupport.createWriterBuilder(request, property);
            writer = writerBuilder.build();
            ExcelWriterSheetBuilder sheetBuilder = writeSupport.createSheetBuilder(writer, request, property);
            WriteSheet writeSheet = sheetBuilder.build();

            int pageNo = 1;
            while (true) {
                List<T> pageData = request.getPageProvider().fetch(pageNo, pageSize);
                if (pageData == null) {
                    throw requestValidator.badRequest("ExcelPageProvider#fetch must not return null");
                }
                if (pageData.isEmpty()) {
                    break;
                }
                writer.write(pageData, writeSheet);
                if (pageData.size() < pageSize) {
                    break;
                }
                pageNo++;
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw requestValidator.businessException("Excel paged export failed: " + exception.getMessage());
        } finally {
            if (writer != null) {
                writer.finish();
            }
        }
    }
}
