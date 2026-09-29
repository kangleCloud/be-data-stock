package com.vita.stockmonitor.dto;

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
                        BigDecimal price, BigDecimal changePercent, BigDecimal amount, String status) {
    }

    public record SeriesPoint(String time, BigDecimal price) {
    }

    public record Stock(String symbol, String code, String name, String market, int sortOrder,
                        Profile profile, Quote quote, List<SeriesPoint> series) {
    }

    public record Dashboard(int schemaVersion, boolean xqEnabled, String tradeDate, List<Stock> stocks) {
    }

    public record AdminStock(String symbol, String code, String name, String market, boolean enabled,
                             int sortOrder, Profile profile) {
    }

    public record ProfileStock(String symbol, String code, String name, String market,
                               String industry, String listingDate, BigDecimal marketCap, String updatedAt) {
    }

    public record RefreshStatus(boolean accepted, String jobId, String status, String startedAt,
                                String finishedAt, String message) {
    }

    public record EnabledRequest(String symbol, Boolean enabled) {
    }

    public record SortRequest(List<String> symbols) {
    }
}
