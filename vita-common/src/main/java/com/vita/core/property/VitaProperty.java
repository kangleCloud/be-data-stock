package com.vita.core.property;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.property
 * @Author: znk
 * @CreateTime: 2026-03-12  21:12:02
 * @Description: 系统属性类，封装了系统相关的属性配置
 * @Version: 1.0
 */
@Component
@ConfigurationProperties(prefix = "vita")
public class VitaProperty {

    @Value("${vita.name:Vita}")
    private String name;

    @Value("${vita.version:1.0.0}")
    private String version;

    @Value("${vita.copyrightYear:2026}")
    private String copyrightYear;

    @Value("${vita.addressEnabled:false}")
    private static boolean addressEnabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCopyrightYear() {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    public static boolean isAddressEnabled() {
        return addressEnabled;
    }

    public static void setAddressEnabled(boolean addressEnabled) {
        VitaProperty.addressEnabled = addressEnabled;
    }
}
