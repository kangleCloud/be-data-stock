package com.vita.poi.excel.model.result;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入汇总结果。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelImportResult {

    private Integer totalRows = 0;

    private Integer successRows = 0;

    private Integer failedRows = 0;

    private Boolean truncated = false;

    private List<ExcelImportError> errors = new ArrayList<>();
}
