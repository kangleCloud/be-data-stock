package com.vita.poi.excel.model.context;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 批处理上下文。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelBatchContext {

    private String fileName;

    private Integer sheetNo;

    private String sheetName;

    private Integer batchNo;

    private Integer startRowIndex;

    private Integer endRowIndex;
}
