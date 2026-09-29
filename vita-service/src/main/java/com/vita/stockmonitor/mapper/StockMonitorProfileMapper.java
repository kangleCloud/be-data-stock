package com.vita.stockmonitor.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.stockmonitor.entity.StockMonitorProfile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMonitorProfileMapper extends BaseMapperX<StockMonitorProfile> {

    @Delete("DELETE FROM stock_monitor_profile WHERE symbol = #{symbol}")
    int deleteBySymbol(String symbol);
}
