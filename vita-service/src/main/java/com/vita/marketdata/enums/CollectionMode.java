package com.vita.marketdata.enums;

/** 单次采集调用的准入模式，仅 scheduler 真实回环入口显式选择 MANUAL。 */
public enum CollectionMode {
    AUTO("auto"), MANUAL("manual");

    private final String headerValue;

    CollectionMode(String headerValue) {
        this.headerValue = headerValue;
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
