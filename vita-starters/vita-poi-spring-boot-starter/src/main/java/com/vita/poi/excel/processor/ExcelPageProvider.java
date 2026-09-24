package com.vita.poi.excel.processor;

import java.util.List;

/**
 * 分页导出数据提供器。
 */
@FunctionalInterface
public interface ExcelPageProvider<T> {

    List<T> fetch(int pageNo, int pageSize);
}
