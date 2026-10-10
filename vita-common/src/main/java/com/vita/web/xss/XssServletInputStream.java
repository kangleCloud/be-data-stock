package com.vita.web.xss;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

/** 独立缓存流，正确报告剩余数据；异步读取监听由当前 AsyncContext 调度。 */
final class XssServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream input;
    private final HttpServletRequest request;
    private ReadListener listener;

    XssServletInputStream(byte[] body, HttpServletRequest request) {
        this.input = new ByteArrayInputStream(body);
        this.request = request;
    }

    @Override
    public int read() {
        return input.read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        return input.read(bytes, offset, length);
    }

    @Override
    public int available() {
        return input.available();
    }

    @Override
    public boolean isFinished() {
        return input.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        Objects.requireNonNull(readListener);
        if (listener != null || !request.isAsyncStarted()) {
            throw new IllegalStateException("读取监听仅允许在异步请求中设置一次");
        }
        listener = readListener;
        request.getAsyncContext().start(() -> {
            try {
                if (!isFinished()) {
                    listener.onDataAvailable();
                }
                if (isFinished()) {
                    listener.onAllDataRead();
                }
            } catch (IOException exception) {
                listener.onError(exception);
            }
        });
    }
}
