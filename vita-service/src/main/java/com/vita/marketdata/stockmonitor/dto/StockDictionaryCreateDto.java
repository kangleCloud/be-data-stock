package com.vita.marketdata.stockmonitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 手工补录交易所字典；股票标识由服务端生成。 */
public record StockDictionaryCreateDto(
        @NotBlank @Pattern(regexp = "SH|SZ|BJ", message = "交易所必须为 SH、SZ 或 BJ") String market,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "股票代码必须为六位数字") String code,
        @NotBlank @Size(max = 100, message = "股票名称最多 100 字") String name) {
}
