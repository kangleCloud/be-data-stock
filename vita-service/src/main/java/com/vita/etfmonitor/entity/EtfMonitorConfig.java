package com.vita.etfmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("etf_monitor_config")
@EqualsAndHashCode(callSuper = true)
public class EtfMonitorConfig extends BaseEntity {
    private String symbol;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
}
