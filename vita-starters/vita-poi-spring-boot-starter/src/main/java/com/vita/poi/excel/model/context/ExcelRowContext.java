package com.vita.poi.excel.model.context;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 行级处理上下文。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelRowContext {

    private String fileName;

    private Integer sheetNo;

    private String sheetName;

    private Integer rowIndex;

    private Map<Integer, String> rawData;
}
