package com.vita.poi.excel.service;

import com.vita.poi.excel.model.request.ExcelExportRequest;
import com.vita.poi.excel.model.request.ExcelPagedExportRequest;

/**
 * Excel 导出服务。
 */
public interface ExcelExportService {

    <T> void export(ExcelExportRequest<T> request);

    <T> void exportByPage(ExcelPagedExportRequest<T> request);
}
