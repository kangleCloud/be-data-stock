package com.vita.marketdata.stockmonitor.mapper;

import com.vita.marketdata.stockmonitor.entity.StockMonitorProfile;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMonitorProfileMapper extends BaseMapperX<StockMonitorProfile> {

    @Delete("DELETE FROM stock_monitor_profile WHERE symbol = #{symbol}")
    int deleteBySymbol(String symbol);
}
