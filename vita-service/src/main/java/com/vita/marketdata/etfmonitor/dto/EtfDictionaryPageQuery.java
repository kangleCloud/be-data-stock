package com.vita.marketdata.etfmonitor.dto;

import com.vita.core.page.PageRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EtfDictionaryPageQuery extends PageRequest {
    @Size(max = 100)
    private String keyword;
    @Pattern(regexp = "(?i)(SH|SZ)?")
    private String market;
    @Size(max = 100)
    private String etfType;
}
