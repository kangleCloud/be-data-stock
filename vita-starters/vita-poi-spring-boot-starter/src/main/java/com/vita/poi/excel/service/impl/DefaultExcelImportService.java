package com.vita.poi.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import com.vita.core.exception.ServiceException;
import com.vita.poi.excel.listener.DefaultExcelImportListener;
import com.vita.poi.excel.model.request.ExcelImportRequest;
import com.vita.poi.excel.model.result.ExcelImportResult;
import com.vita.poi.excel.processor.ExcelImportProcessor;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.service.ExcelImportService;
import com.vita.poi.excel.support.ExcelReadSupport;
import com.vita.poi.excel.support.ExcelRequestValidator;

/**
 * 默认 Excel 导入服务。
 */
public class DefaultExcelImportService implements ExcelImportService {

    private final PoiExcelProperty property;
    private final ExcelRequestValidator requestValidator;
    private final ExcelReadSupport readSupport;

    public DefaultExcelImportService(PoiExcelProperty property,
                                     ExcelRequestValidator requestValidator,
                                     ExcelReadSupport readSupport) {
        this.property = property;
        this.property.normalizeAndValidate();
        this.requestValidator = requestValidator;
        this.readSupport = readSupport;
    }

    @Override
    public <T> ExcelImportResult importExcel(ExcelImportRequest<T> request, ExcelImportProcessor<T> processor) {
        requestValidator.validateImportRequest(request, processor, property);
        DefaultExcelImportListener<T> listener = new DefaultExcelImportListener<>(request, processor, property, readSupport);
        try {
            ExcelReaderBuilder readerBuilder = EasyExcel.read(request.getInputStream(), request.getHeadClass(), listener);
            readSupport.applyWorkbookDefaults(readerBuilder, request, property);
            ExcelReaderSheetBuilder sheetBuilder = readSupport.createSheetBuilder(readerBuilder, request, property);
            sheetBuilder.doRead();
            return listener.buildResult();
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw requestValidator.businessException("Excel import failed: " + readSupport.resolveErrorMessage(exception));
        }
    }
}
