package com.vita.stockmonitor.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.stockmonitor.entity.StockSymbolDictionary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockSymbolDictionaryMapper extends BaseMapperX<StockSymbolDictionary> {

    @Insert("""
            <script>
            INSERT INTO stock_symbol_dictionary (symbol, code, name, market, create_time)
            VALUES
            <foreach collection='stocks' item='stock' separator=','>
              (#{stock.symbol}, #{stock.code}, #{stock.name}, #{stock.market}, NOW())
            </foreach>
            ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name),
                                    market = VALUES(market), is_deleted = 0
            </script>
            """)
    int upsertBatch(@Param("stocks") List<StockSymbolDictionary> stocks);
}
