package com.vita.marketdata.market.dto;

import jakarta.validation.constraints.*;

public record MarketIndexUpdateRequest(
        @NotBlank @Pattern(regexp = "sh000001|sz399001|sh000300|sz399006|sh000688") String code,
        @NotNull Boolean enabled, @NotNull @Min(1) @Max(5) Integer sortOrder) {
}
