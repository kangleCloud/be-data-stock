package com.vita.marketdata.etfmonitor.dto;

import com.vita.marketdata.etfmonitor.entity.EtfSymbolDictionary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    public record ProfileDetail(EtfSymbolDictionary dictionary, ProfileRow profile,
                                Object assetAllocation) {
    }

    public record ProfileRow(String symbol, String code, String name, String market,
                             String exchange, String etfType, String listingStatus,
                             LocalDate listingDate, String manager, String custodian,
                             BigDecimal shareCount, LocalDate shareDate,
                             String trackingIndexCode, String trackingIndexName,
                             String updatedAt, String fullName, String fundType,
                             String investmentType, String fundManager, LocalDate establishedDate,
                             String performanceBenchmark, String source) {
    }

    public record EnabledRequest(@NotBlank @Pattern(regexp = "(SH|SZ)[0-9]{6}") String symbol,
                                 @NotNull Boolean enabled) {
    }

    public record SortRequest(@NotNull @Size(max = 10)
                              List<@NotBlank @Pattern(regexp = "(SH|SZ)[0-9]{6}") String> symbols) {
    }
}
