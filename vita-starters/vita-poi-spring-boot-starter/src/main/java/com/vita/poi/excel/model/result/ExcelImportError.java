package com.vita.poi.excel.model.result;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Excel 导入错误明细。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelImportError {

    private Integer rowIndex;

    private String fieldName;

    private String message;

    private Map<Integer, String> rawData;
}
