package com.vita.i18n.constant;

import com.vita.core.exception.ErrorCode;
import com.vita.core.exception.GlobalErrorCode;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.i18n.constant
 * @Author: znk
 * @CreateTime: 2026-03-20  23:31:30
 * @Description: 国际化常量
 * @Version: 1.0
 */
public class I18nConstant {
    public static final String DEFAULT_LANGUAGE = "zh-CN";
    public static final String ERROR_CODE_PREFIX = "error.";
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;
    public static final Map<String, Locale> LOCALE_MAP;
    public static final List<Locale> SUPPORTED_LOCALES;
    private static final Map<Integer, String> DEFAULT_ERROR_MESSAGE_MAP;

    private I18nConstant() {
    }

    static {
        Map<String, Locale> localeMap = new HashMap<>();
        localeMap.put(DEFAULT_LANGUAGE, Locale.SIMPLIFIED_CHINESE);
        localeMap.put("en-US", Locale.US);
        LOCALE_MAP = Collections.unmodifiableMap(localeMap);
        SUPPORTED_LOCALES = List.copyOf(LOCALE_MAP.values());
        DEFAULT_ERROR_MESSAGE_MAP = Collections.unmodifiableMap(loadDefaultErrorMessages());
    }

    public static String normalizeLanguageTag(String languageTag) {
        if (!StringUtils.hasText(languageTag)) {
            return DEFAULT_LANGUAGE;
        }
        String candidate = languageTag.trim();
        for (String supportedTag : LOCALE_MAP.keySet()) {
            if (supportedTag.equalsIgnoreCase(candidate)) {
                return supportedTag;
            }
        }
        return candidate;
    }

    public static Locale resolveLocale(String languageTag) {
        return LOCALE_MAP.get(normalizeLanguageTag(languageTag));
    }

    public static Locale resolveRequestLocale(String acceptLanguage, Locale defaultLocale) {
        if (!StringUtils.hasText(acceptLanguage)) {
            return defaultLocale;
        }
        try {
            Locale locale = Locale.lookup(Locale.LanguageRange.parse(acceptLanguage), SUPPORTED_LOCALES);
            if (locale != null) {
                return locale;
            }
        } catch (IllegalArgumentException ignored) {
        }
        Locale fallback = resolveLocale(acceptLanguage);
        return fallback == null ? defaultLocale : fallback;
    }

    public static String buildErrorCodeKey(Integer code) {
        return ERROR_CODE_PREFIX + code;
    }

    public static boolean isDefaultErrorMessage(Integer code, String message) {
        if (code == null || !StringUtils.hasText(message)) {
            return false;
        }
        return message.equals(DEFAULT_ERROR_MESSAGE_MAP.get(code));
    }

    private static Map<Integer, String> loadDefaultErrorMessages() {
        Map<Integer, String> messages = new HashMap<>();
        for (Field field : GlobalErrorCode.class.getDeclaredFields()) {
            if (!ErrorCode.class.equals(field.getType())) {
                continue;
            }
            try {
                ErrorCode errorCode = (ErrorCode) field.get(null);
                if (errorCode != null) {
                    messages.put(errorCode.getCode(), errorCode.getMsg());
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return messages;
    }
}
