package com.vita.stockmonitor.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 已同步股票资料分页条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockProfilePageQuery extends PageRequest {
    private String keyword;
    private String industry;
}
