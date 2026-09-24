package com.vita.poi.excel.config;

import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.service.ExcelExportService;
import com.vita.poi.excel.service.ExcelFillService;
import com.vita.poi.excel.service.ExcelImportService;
import com.vita.poi.excel.service.impl.DefaultExcelExportService;
import com.vita.poi.excel.service.impl.DefaultExcelFillService;
import com.vita.poi.excel.service.impl.DefaultExcelImportService;
import com.vita.poi.excel.support.ExcelFillSupport;
import com.vita.poi.excel.support.ExcelReadSupport;
import com.vita.poi.excel.support.ExcelRequestValidator;
import com.vita.poi.excel.support.ExcelWriteSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Excel starter 自动装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(PoiExcelProperty.class)
@ConditionalOnProperty(prefix = "vita.poi.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PoiExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelRequestValidator excelRequestValidator() {
        return new ExcelRequestValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelReadSupport excelReadSupport() {
        return new ExcelReadSupport();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelWriteSupport excelWriteSupport() {
        return new ExcelWriteSupport();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelFillSupport excelFillSupport() {
        return new ExcelFillSupport();
    }

    @Bean
    @ConditionalOnMissingBean(ExcelImportService.class)
    public ExcelImportService excelImportService(PoiExcelProperty property,
                                                 ExcelRequestValidator requestValidator,
                                                 ExcelReadSupport readSupport) {
        return new DefaultExcelImportService(property, requestValidator, readSupport);
    }

    @Bean
    @ConditionalOnMissingBean(ExcelExportService.class)
    public ExcelExportService excelExportService(PoiExcelProperty property,
                                                 ExcelRequestValidator requestValidator,
                                                 ExcelWriteSupport writeSupport) {
        return new DefaultExcelExportService(property, requestValidator, writeSupport);
    }

    @Bean
    @ConditionalOnMissingBean(ExcelFillService.class)
    public ExcelFillService excelFillService(PoiExcelProperty property,
                                             ExcelRequestValidator requestValidator,
                                             ExcelFillSupport fillSupport) {
        return new DefaultExcelFillService(property, requestValidator, fillSupport);
    }
}
