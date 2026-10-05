package com.vita.marketdata.enums;

import java.time.Duration;

/** 内部采集任务与 Python 路径、等待预算绑定，不暴露 HTTP 任务选择参数。 */
public enum PythonJobKind {
    CALENDAR("calendar", Duration.ofSeconds(90)),
    MARKET("market", Duration.ofSeconds(1440)),
    MONITOR("monitor", Duration.ofSeconds(360)),
    ETF("etf", Duration.ofSeconds(360));

    private final String path;
    private final Duration readTimeout;

    PythonJobKind(String path, Duration readTimeout) {
        this.path = path;
        this.readTimeout = readTimeout;
    }

    public String getPath() { return path; }
    public Duration getReadTimeout() { return readTimeout; }
}
