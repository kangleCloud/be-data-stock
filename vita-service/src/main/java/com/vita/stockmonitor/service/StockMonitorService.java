package com.vita.stockmonitor.service;

import com.vita.core.page.PageResponse;
import com.vita.stockmonitor.dto.*;

import java.util.List;

public interface StockMonitorService {
    PageResponse<StockMonitorDtos.AdminStock> pageMonitor(StockMonitorPageQuery query);

    PageResponse<StockMonitorDtos.DictionaryItem> pageDictionary(StockDictionaryPageQuery query);

    StockMonitorDtos.DictionaryItem addDictionaryStock(StockDictionaryCreateDto request);

    PageResponse<StockMonitorDtos.ProfileStock> pageProfile(StockProfilePageQuery query);

    List<StockMonitorDtos.DictionaryItem> searchDictionary(String keyword, int limit);

    List<StockMonitorDtos.AdminStock> listAdmin();

    void setEnabled(String symbol, Boolean enabled);

    void sort(List<String> symbols);

    StockMonitorDtos.Dashboard dashboard();

    List<String> enabledSymbols();

    void rebuildEnabledCache();
}
