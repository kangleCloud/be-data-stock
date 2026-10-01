package com.vita.controller.local;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;

/** 本机手动入口只接受真实 TCP loopback 直连，不信任代理声明的来源。 */
public final class LoopbackRequestGuard {
    private LoopbackRequestGuard() {
    }

    public static void requireLoopback(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if ((!"127.0.0.1".equals(address) && !"::1".equals(address)
                && !"0:0:0:0:0:0:0:1".equals(address))
                || request.getHeader("Forwarded") != null
                || request.getHeader("X-Forwarded-For") != null
                || request.getHeader("X-Real-IP") != null) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN);
        }
    }
}
