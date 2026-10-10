package com.vita.marketdata.stockmonitor.dto;

import com.vita.core.page.PageRequest;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 已同步股票资料分页条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockProfilePageQuery extends PageRequest {
    @Size(max = 100)
    private String keyword;
    @Size(max = 100)
    private String industry;
}
