package com.vita.poi.excel.service;

import com.vita.poi.excel.model.request.ExcelFillRequest;

/**
 * Excel 模板填充服务。
 */
public interface ExcelFillService {

    void fill(ExcelFillRequest request);
}
