package com.vita.pythonjobs.dto;

/** Python 手动任务同步执行后的 V1 终态响应。 */
public record PythonRunResult(String kind, String state, String outcome,
                              String startedAt, String finishedAt, String message) {
}
