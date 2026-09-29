package com.vita.stockmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统级监控启停与排序。 */
@Data
@TableName("stock_monitor_config")
@EqualsAndHashCode(callSuper = true)
public class StockMonitorConfig extends BaseEntity {
    private String symbol;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
}
