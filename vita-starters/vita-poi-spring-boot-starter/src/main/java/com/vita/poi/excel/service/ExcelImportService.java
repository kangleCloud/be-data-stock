package com.vita.poi.excel.service;

import com.vita.poi.excel.model.request.ExcelImportRequest;
import com.vita.poi.excel.model.result.ExcelImportResult;
import com.vita.poi.excel.processor.ExcelImportProcessor;

/**
 * Excel 导入服务。
 */
public interface ExcelImportService {

    <T> ExcelImportResult importExcel(ExcelImportRequest<T> request, ExcelImportProcessor<T> processor);
}
