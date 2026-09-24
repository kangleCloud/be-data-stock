package com.vita.log.resolver;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.service.IpLocationService;
import com.vita.utils.web.ServletUtils;
import com.vita.utils.web.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.resolver
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 请求客户端信息解析器
 * @Version: 1.0
 */
@Component
public class RequestClientInfoResolver {

    private final IpLocationService ipLocationService;

    public RequestClientInfoResolver(IpLocationService ipLocationService) {
        this.ipLocationService = ipLocationService;
    }

    /**
     * 解析当前线程绑定请求的客户端信息。
     *
     * @return 客户端信息快照
     */
    public RequestClientInfo resolveCurrentRequest() {
        try {
            return resolve(ServletUtils.getRequest());
        } catch (Exception ignored) {
            return resolve(null);
        }
    }

    /**
     * 基于指定请求解析客户端信息。
     *
     * @param request 请求对象
     * @return 客户端信息快照
     */
    public RequestClientInfo resolve(HttpServletRequest request) {
        RequestClientInfo requestClientInfo = new RequestClientInfo();
        requestClientInfo.setRequestTime(LocalDateTime.now());
        String ip = request == null ? "unknown" : IpUtils.getIpAddr(request);
        requestClientInfo.setIp(ip);
        requestClientInfo.setLocation(ipLocationService.resolveFast(ip));

        String userAgentValue = request == null ? null : request.getHeader("User-Agent");
        UserAgent userAgent = CharSequenceUtil.isBlank(userAgentValue) ? null : UserAgentUtil.parse(userAgentValue);
        requestClientInfo.setBrowser(userAgent == null || userAgent.getBrowser() == null ? "Unknown" : userAgent.getBrowser().getName());
        requestClientInfo.setOs(userAgent == null || userAgent.getOs() == null ? "Unknown" : userAgent.getOs().getName());
        return requestClientInfo;
    }
}
