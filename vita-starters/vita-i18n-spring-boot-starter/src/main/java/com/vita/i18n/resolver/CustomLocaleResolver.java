package com.vita.i18n.resolver;

import com.vita.i18n.constant.I18nConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.i18n.resolver
 * @Author: znk
 * @CreateTime: 2026-03-20  23:46:52
 * @Description: 自定义区域信息解析器
 * @Version: 1.0
 */
@Getter
public class CustomLocaleResolver implements LocaleResolver {

    private final Locale defaultLocale;

    public CustomLocaleResolver(Locale defaultLocale) {
        this.defaultLocale = defaultLocale == null ? I18nConstant.DEFAULT_LOCALE : defaultLocale;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest httpServletRequest) {
        return I18nConstant.resolveRequestLocale(httpServletRequest.getHeader("Accept-Language"), defaultLocale);
    }

    @Override
    public void setLocale(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Locale locale) {
    }
}
