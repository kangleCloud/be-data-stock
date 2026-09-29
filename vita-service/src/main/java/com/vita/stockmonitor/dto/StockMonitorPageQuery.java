package com.vita.stockmonitor.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 监控配置分页条件；enabled 来自 MySQL 配置表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockMonitorPageQuery extends PageRequest {
    private String keyword;
    private Boolean enabled;
}
