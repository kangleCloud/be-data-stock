package com.vita.stockmonitor.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 交易所股票字典分页条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockDictionaryPageQuery extends PageRequest {
    private String keyword;
    private String market;
}
