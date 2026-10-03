package com.vita.etfmonitor.dto;

import com.vita.etfmonitor.entity.EtfMonitorProfile;
import com.vita.etfmonitor.entity.EtfSymbolDictionary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 管理端只传递已持久化的 ETF 字典与配置，不携带实时行情。 */
public final class EtfMonitorDtos {
    private EtfMonitorDtos() {
    }

    public record AdminEtf(String symbol, String code, String name, String market,
                           boolean enabled, int sortOrder) {
    }

    public record ProfileDetail(EtfSymbolDictionary dictionary, EtfMonitorProfile profile,
                                Object assetAllocation) {
    }

    public record ProfileRow(String symbol, String code, String name, String market,
                             String exchange, String etfType, String listingStatus,
                             LocalDate listingDate, String manager, String custodian,
                             BigDecimal shareCount, LocalDate shareDate,
                             String trackingIndexCode, String trackingIndexName,
                             String updatedAt) {
    }

    public record EnabledRequest(String symbol, Boolean enabled) {
    }

    public record SortRequest(List<String> symbols) {
    }
}
