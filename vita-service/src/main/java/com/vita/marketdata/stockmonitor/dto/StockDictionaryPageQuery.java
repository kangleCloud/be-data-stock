package com.vita.marketdata.stockmonitor.dto;

import com.vita.core.page.PageRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 交易所股票字典分页条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockDictionaryPageQuery extends PageRequest {
    @Size(max = 100)
    private String keyword;
    @Pattern(regexp = "(?i)(SH|SZ|BJ)?")
    private String market;
}
