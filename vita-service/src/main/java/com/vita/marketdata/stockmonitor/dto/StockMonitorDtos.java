package com.vita.marketdata.stockmonitor.dto;

import java.math.BigDecimal;
import java.util.List;

/** 个股监控 V1 公共传输结构。 */
public final class StockMonitorDtos {
    private StockMonitorDtos() {
    }

    public record DictionaryItem(String symbol, String code, String name, String market) {
    }

    public record Profile(String industry, String listingDate, BigDecimal marketCap, String updatedAt) {
    }

    public record Quote(String source, String sourceTime, String collectedAt, String tradeDate,
                        BigDecimal price, BigDecimal changePercent, BigDecimal amount,
                        BigDecimal low, BigDecimal high, BigDecimal open, BigDecimal limitUp,
                        BigDecimal limitDown, BigDecimal averagePrice, BigDecimal volume,
                        BigDecimal previousClose, String status) {
    }

    public record SeriesPoint(String time, BigDecimal price) {
    }

    public record FundPoint(String collectedAt, BigDecimal inflow, BigDecimal outflow, BigDecimal netAmount) {
    }

    public record Stock(String symbol, String code, String name, String market, int sortOrder,
                        Profile profile, Quote quote, List<SeriesPoint> series,
                        String effectiveTradeDate, String dataStatus, boolean closeConfirmed,
                        List<FundPoint> fundSeries, String fundFlowStatus, String fundFlowMessage) {
    }

    public record Dashboard(int schemaVersion, String stateId, boolean xqEnabled,
                            String tradeDate, List<Stock> stocks) {
    }

    public record AdminStock(String symbol, String code, String name, String market, boolean enabled,
                             int sortOrder, Profile profile) {
    }

    public record ProfileStock(String symbol, String code, String name, String market,
                               String industry, String listingDate, BigDecimal marketCap, String updatedAt) {
    }

    public record RefreshStatus(boolean accepted, String status, String startedAt,
                                String finishedAt, String message) {
    }

    public record EnabledRequest(String symbol, Boolean enabled) {
    }

    public record SortRequest(List<String> symbols) {
    }
}
