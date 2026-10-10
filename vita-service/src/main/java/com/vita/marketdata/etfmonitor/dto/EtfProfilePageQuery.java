package com.vita.marketdata.etfmonitor.dto;

import com.vita.core.page.PageRequest;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EtfProfilePageQuery extends PageRequest {
    @Size(max = 100)
    private String keyword;
    @Size(max = 100)
    private String fundType;
    @Size(max = 32)
    private String trackingIndexCode;
}
