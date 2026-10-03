package com.vita.etfmonitor.dto;

/** 同步等待后的真实终态；无需 Java 侧 jobId。 */
public record EtfRefreshResult(String status, String startedAt, String finishedAt, String message) {
}
