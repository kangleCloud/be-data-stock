package com.vita.etfmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("etf_symbol_dictionary")
@EqualsAndHashCode(callSuper = true)
public class EtfSymbolDictionary extends BaseEntity {
    private String symbol;
    private String code;
    private String name;
    private String market;
    private String exchange;
    private String etfType;
    private String listingStatus;
    private LocalDate listingDate;
    private String trackingIndexCode;
    private String trackingIndexName;
    private String source;
    private LocalDateTime syncedAt;
    private String remark;
}
