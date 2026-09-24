package com.vita.poi.excel.support;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.poi.excel.model.request.ExcelExportRequest;
import com.vita.poi.excel.model.request.ExcelFillRequest;
import com.vita.poi.excel.model.request.ExcelImportRequest;
import com.vita.poi.excel.model.request.ExcelPagedExportRequest;
import com.vita.poi.excel.processor.ExcelImportProcessor;
import com.vita.poi.excel.property.PoiExcelProperty;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * 请求参数校验。
 */
public class ExcelRequestValidator {

    public <T> void validateImportRequest(ExcelImportRequest<T> request,
                                          ExcelImportProcessor<T> processor,
                                          PoiExcelProperty property) {
        property.normalizeAndValidate();
        if (request == null) {
            throw badRequest("ExcelImportRequest must not be null");
        }
        if (request.getInputStream() == null) {
            throw badRequest("Excel import inputStream must not be null");
        }
        if (request.getHeadClass() == null) {
            throw badRequest("Excel import headClass must not be null");
        }
        if (processor == null) {
            throw badRequest("ExcelImportProcessor must not be null");
        }
        if (request.getHeadRowNumber() != null && request.getHeadRowNumber() <= 0) {
            throw badRequest("Excel import headRowNumber must be greater than 0");
        }
        if (request.getBatchSize() != null && request.getBatchSize() <= 0) {
            throw badRequest("Excel import batchSize must be greater than 0");
        }
    }

    public <T> void validateExportRequest(ExcelExportRequest<T> request, PoiExcelProperty property) {
        property.normalizeAndValidate();
        if (request == null) {
            throw badRequest("ExcelExportRequest must not be null");
        }
        if (request.getOutputStream() == null) {
            throw badRequest("Excel export outputStream must not be null");
        }
        if (request.getHeadClass() == null) {
            throw badRequest("Excel export headClass must not be null");
        }
        if (request.getRelativeHeadRowIndex() != null && request.getRelativeHeadRowIndex() < 0) {
            throw badRequest("Excel export relativeHeadRowIndex must not be negative");
        }
        validateColumnRules(
                request.getIncludeColumnIndexes(),
                request.getExcludeColumnIndexes(),
                request.getIncludeColumnFieldNames(),
                request.getExcludeColumnFieldNames()
        );
    }

    public <T> void validatePagedExportRequest(ExcelPagedExportRequest<T> request, PoiExcelProperty property) {
        property.normalizeAndValidate();
        if (request == null) {
            throw badRequest("ExcelPagedExportRequest must not be null");
        }
        if (request.getOutputStream() == null) {
            throw badRequest("Excel paged export outputStream must not be null");
        }
        if (request.getHeadClass() == null) {
            throw badRequest("Excel paged export headClass must not be null");
        }
        if (request.getPageProvider() == null) {
            throw badRequest("Excel paged export pageProvider must not be null");
        }
        if (request.getPageSize() != null && request.getPageSize() <= 0) {
            throw badRequest("Excel paged export pageSize must be greater than 0");
        }
        if (request.getRelativeHeadRowIndex() != null && request.getRelativeHeadRowIndex() < 0) {
            throw badRequest("Excel paged export relativeHeadRowIndex must not be negative");
        }
        validateColumnRules(
                request.getIncludeColumnIndexes(),
                request.getExcludeColumnIndexes(),
                request.getIncludeColumnFieldNames(),
                request.getExcludeColumnFieldNames()
        );
    }

    public void validateFillRequest(ExcelFillRequest request, PoiExcelProperty property) {
        property.normalizeAndValidate();
        if (request == null) {
            throw badRequest("ExcelFillRequest must not be null");
        }
        if (request.getTemplateInputStream() == null) {
            throw badRequest("Excel fill templateInputStream must not be null");
        }
        if (request.getOutputStream() == null) {
            throw badRequest("Excel fill outputStream must not be null");
        }
        if (request.getObjectData() == null && request.getListData() == null) {
            throw badRequest("Excel fill requires objectData or listData");
        }
    }

    public ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    public ServiceException businessException(String message) {
        return new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), message);
    }

    private void validateColumnRules(Collection<Integer> includeColumnIndexes,
                                     Collection<Integer> excludeColumnIndexes,
                                     Collection<String> includeColumnFieldNames,
                                     Collection<String> excludeColumnFieldNames) {
        if (!CollectionUtils.isEmpty(includeColumnIndexes) && !CollectionUtils.isEmpty(excludeColumnIndexes)) {
            throw badRequest("includeColumnIndexes and excludeColumnIndexes cannot both be set");
        }
        if (!CollectionUtils.isEmpty(includeColumnFieldNames) && !CollectionUtils.isEmpty(excludeColumnFieldNames)) {
            throw badRequest("includeColumnFieldNames and excludeColumnFieldNames cannot both be set");
        }
    }
}
