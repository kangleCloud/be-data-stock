package com.vita.etfmonitor.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EtfProfilePageQuery extends PageRequest {
    private String keyword;
    private String etfType;
    private String trackingIndexCode;
}
