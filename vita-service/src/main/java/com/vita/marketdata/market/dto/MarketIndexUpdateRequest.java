package com.vita.marketdata.market.dto;

public record MarketIndexUpdateRequest(String code, Boolean enabled, Integer sortOrder) {
}
