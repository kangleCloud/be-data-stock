package com.vita.market.dto;

public record MarketIndexUpdateRequest(String code, Boolean enabled, Integer sortOrder) {
}
