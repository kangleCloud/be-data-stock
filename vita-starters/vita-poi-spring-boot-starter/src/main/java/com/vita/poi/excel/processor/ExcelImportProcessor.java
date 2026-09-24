package com.vita.poi.excel.processor;

import com.vita.poi.excel.model.context.ExcelBatchContext;
import com.vita.poi.excel.model.context.ExcelRowContext;

import java.util.Collections;
import java.util.List;

/**
 * 业务导入回调。
 */
public interface ExcelImportProcessor<T> {

    default List<String> validate(T row, ExcelRowContext context) {
        return Collections.emptyList();
    }

    void saveBatch(List<T> rows, ExcelBatchContext context);
}
