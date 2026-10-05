package com.vita.marketdata.etfmonitor.mapper;

import com.vita.marketdata.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EtfSymbolDictionaryMapper extends BaseMapperX<EtfSymbolDictionary> {
    @Insert("""
            <script>
            INSERT INTO etf_symbol_dictionary
              (symbol, code, name, market, exchange, etf_type, listing_status, listing_date,
               tracking_index_code, tracking_index_name, source, synced_at, create_time)
            VALUES
            <foreach collection='etfs' item='etf' separator=','>
              (#{etf.symbol}, #{etf.code}, #{etf.name}, #{etf.market}, #{etf.exchange},
               #{etf.etfType}, #{etf.listingStatus}, #{etf.listingDate},
               #{etf.trackingIndexCode}, #{etf.trackingIndexName}, #{etf.source},
               #{etf.syncedAt}, NOW())
            </foreach>
            ON DUPLICATE KEY UPDATE code=VALUES(code), name=VALUES(name), market=VALUES(market),
              exchange=VALUES(exchange), etf_type=VALUES(etf_type),
              listing_status=VALUES(listing_status), listing_date=VALUES(listing_date),
              source=VALUES(source), synced_at=VALUES(synced_at), is_deleted=0
            </script>
            """)
    int upsertBatch(@Param("etfs") List<EtfSymbolDictionary> etfs);
}
