package com.vita.marketdata.stockmonitor.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 仅保存选中股票的有限雪球基础资料。 */
@Data
@TableName("stock_monitor_profile")
@EqualsAndHashCode(callSuper = true)
public class StockMonitorProfile extends BaseEntity {
    private String symbol;
    private String industry;
    private String listingDate;
    private BigDecimal marketCap;
    @TableField("profile_updated_at")
    private String updatedAt;
    private String remark;
}
