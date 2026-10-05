package com.vita.marketdata.etfmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("etf_monitor_profile")
@EqualsAndHashCode(callSuper = true)
public class EtfMonitorProfile extends BaseEntity {
    private String symbol;
    private String exchange;
    private String etfType;
    private String fullName;
    private String fundType;
    private String investmentType;
    private String fundManager;
    private LocalDate establishedDate;
    private String performanceBenchmark;
    private String source;
    private String listingStatus;
    private LocalDate listingDate;
    private String manager;
    private String custodian;
    private BigDecimal shareCount;
    private LocalDate shareDate;
    private String trackingIndexCode;
    private String trackingIndexName;
    private LocalDateTime profileUpdatedAt;
    private String remark;
}
