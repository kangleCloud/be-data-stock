package com.vita.marketdata.etfmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("etf_asset_allocation_report")
@EqualsAndHashCode(callSuper = true)
public class EtfAssetAllocationReport extends BaseEntity {
    private String symbol;
    private LocalDate requestedReportPeriod;
    private String source;
    private LocalDateTime collectedAt;
    private String categoriesJson;
    private String remark;
}
