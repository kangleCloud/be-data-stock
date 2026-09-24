package com.vita.poi.excel.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Excel starter 配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vita.poi.excel")
public class PoiExcelProperty {

    private boolean enabled = true;

    private int defaultHeadRowNumber = 1;

    private int defaultImportBatchSize = 100;

    private int defaultExportPageSize = 5000;

    private int maxErrorRows = 200;

    private boolean trimCellValue = true;

    private boolean ignoreEmptyRow = true;

    private boolean autoCloseStream = false;

    private boolean inMemory = false;

    private boolean useDefaultStyle = true;

    private String defaultSheetName = "Sheet1";

    public void normalizeAndValidate() {
        if (!StringUtils.hasText(defaultSheetName)) {
            defaultSheetName = "Sheet1";
        } else {
            defaultSheetName = defaultSheetName.trim();
        }
        validatePositive(defaultHeadRowNumber, "vita.poi.excel.default-head-row-number");
        validatePositive(defaultImportBatchSize, "vita.poi.excel.default-import-batch-size");
        validatePositive(defaultExportPageSize, "vita.poi.excel.default-export-page-size");
        validatePositive(maxErrorRows, "vita.poi.excel.max-error-rows");
    }

    private void validatePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalStateException(propertyName + " must be greater than 0");
        }
    }
}
