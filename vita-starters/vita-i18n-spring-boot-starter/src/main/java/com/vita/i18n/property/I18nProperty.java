package com.vita.i18n.property;

import com.vita.i18n.constant.I18nConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.i18n.property
 * @Author: znk
 * @CreateTime: 2026-03-20  23:28:11
 * @Description: 国际化属性类
 * @Version: 1.0
 */
@Data
@ConfigurationProperties(prefix = "vita.i18n")
public class I18nProperty {
    /**
     * 是否启用
     */
    private boolean enabled = false;
    /**
     * 默认区域
     */
    private String defaultLanguage = I18nConstant.DEFAULT_LANGUAGE;

    public void normalizeAndValidate() {
        if (!StringUtils.hasText(defaultLanguage)) {
            defaultLanguage = I18nConstant.DEFAULT_LANGUAGE;
        }
        defaultLanguage = I18nConstant.normalizeLanguageTag(defaultLanguage);
        if (I18nConstant.resolveLocale(defaultLanguage) == null) {
            throw new IllegalStateException("Unsupported default language: " + defaultLanguage);
        }
    }

    public Locale resolveDefaultLocale() {
        normalizeAndValidate();
        return I18nConstant.resolveLocale(defaultLanguage);
    }
}
