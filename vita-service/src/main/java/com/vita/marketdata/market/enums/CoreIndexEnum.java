package com.vita.marketdata.market.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** 原始快照固定五个核心指数；展示启停和顺序仍由数据库配置决定。 */
public enum CoreIndexEnum {
    SHANGHAI("sh000001", "上证指数"),
    SHENZHEN("sz399001", "深证成指"),
    CSI300("sh000300", "沪深300"),
    CHINEXT("sz399006", "创业板指"),
    STAR50("sh000688", "科创50");

    private static final Set<String> CODES = Arrays.stream(values())
            .map(CoreIndexEnum::getCode).collect(Collectors.toUnmodifiableSet());
    private final String code;
    private final String name;

    CoreIndexEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public static Set<String> codes() { return CODES; }
}
