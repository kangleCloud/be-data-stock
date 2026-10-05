package com.vita.marketdata.stockmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 交易所股票字典，不含雪球资料。 */
@Data
@TableName("stock_symbol_dictionary")
@EqualsAndHashCode(callSuper = true)
public class StockSymbolDictionary extends BaseEntity {
    private String symbol;
    private String code;
    private String name;
    private String market;
    private String remark;
}
