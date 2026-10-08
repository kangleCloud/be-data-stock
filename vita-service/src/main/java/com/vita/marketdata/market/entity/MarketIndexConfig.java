package com.vita.marketdata.market.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 五只固定核心指数的显示配置。 */
@Data
@TableName("market_index_config")
@EqualsAndHashCode(callSuper = true)
public class MarketIndexConfig extends BaseEntity {
    private String code;
    private String name;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
}
