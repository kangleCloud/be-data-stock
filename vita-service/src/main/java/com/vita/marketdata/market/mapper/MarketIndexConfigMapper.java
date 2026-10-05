package com.vita.marketdata.market.mapper;

import com.vita.marketdata.market.entity.MarketIndexConfig;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketIndexConfigMapper extends BaseMapperX<MarketIndexConfig> {
    @Select("SELECT * FROM market_index_config WHERE is_deleted=0 ORDER BY sort_order,code FOR UPDATE")
    List<MarketIndexConfig> selectForUpdate();
}
