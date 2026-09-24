package com.vita.market.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 市场快照只读服务。
 */
public interface MarketSnapshotService {

    JsonNode getSnapshot();
}
