package com.vita.marketdata.stockmonitor.service;

import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.property.StockMonitorProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class StockMonitorPythonClientTest {
    @ParameterizedTest
    @EnumSource(CollectionMode.class)
    void dictionaryAndProfilesCarryExplicitModeAndToken(CollectionMode mode) {
        var builder=RestClient.builder(); var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://python.test/internal/stock-monitor/v1/exchange-dictionary"))
                .andExpect(header("X-Collection-Mode",mode.getHeaderValue())).andExpect(header("X-Internal-Token","test-only"))
                .andExpect(content().json("{}"))
                .andRespond(withSuccess("{\"schemaVersion\":1,\"stocks\":[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"market\":\"SH\",\"name\":\"中文\"}]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://python.test/internal/stock-monitor/v1/profiles"))
                .andExpect(header("X-Collection-Mode",mode.getHeaderValue())).andExpect(header("X-Internal-Token","test-only"))
                .andExpect(content().json("{\"symbols\":[\"SH600000\"]}"))
                .andRespond(withSuccess("{\"schemaVersion\":1,\"profiles\":[]}",MediaType.APPLICATION_JSON));
        var client=new StockMonitorPythonClient(property(),builder.build());
        assertEquals("中文",client.exchangeDictionary(mode).get(0).name());
        assertEquals(List.of(),client.profiles(List.of("SH600000"),mode)); server.verify();
    }

    @Test
    void defaultOperationsAreAutoAndEmptyProfilesMakeNoRequest() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andExpect(header("X-Collection-Mode","auto"))
                .andRespond(withSuccess("{\"schemaVersion\":1,\"stocks\":[]}",MediaType.APPLICATION_JSON));
        server.expect(anything()).andExpect(header("X-Collection-Mode","auto"))
                .andRespond(withSuccess("{\"schemaVersion\":1,\"profiles\":[]}",MediaType.APPLICATION_JSON));
        var client=new StockMonitorPythonClient(property(),builder.build());
        client.exchangeDictionary();client.profiles(List.of("SH600000"));client.profiles(List.of(),CollectionMode.MANUAL);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"409,423","429,429","401,503","403,503","400,400","422,400","503,503"})
    void errorsAreSafeAndKeepProtectionCodes(int http,int code) {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(http)).body("secret-source-test"));
        var error=assertThrows(ServiceException.class,()->new StockMonitorPythonClient(property(),builder.build()).exchangeDictionary(CollectionMode.MANUAL));
        assertEquals(code,error.getCode());assertFalse(error.getMessage().contains("secret-source-test"));server.verify();
    }

    @Test
    void timeoutRemainsGatewayTimeout() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withException(new java.net.SocketTimeoutException("test timeout")));
        assertEquals(504,assertThrows(ServiceException.class,()->new StockMonitorPythonClient(property(),builder.build())
                .profiles(List.of("SH600000"),CollectionMode.MANUAL)).getCode());server.verify();
    }

    @Test
    void manualThenDefaultCallOnSameClientCannotLeakMode() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        for(String mode:List.of("manual","auto")) {
            server.expect(anything()).andExpect(header("X-Collection-Mode",mode))
                    .andRespond(withSuccess("{\"schemaVersion\":1,\"stocks\":[]}",MediaType.APPLICATION_JSON));
        }
        var client=new StockMonitorPythonClient(property(),builder.build());
        client.exchangeDictionary(CollectionMode.MANUAL);client.exchangeDictionary();server.verify();
    }

    private StockMonitorProperty property() {
        var property=new StockMonitorProperty();property.setPythonBaseUrl("http://python.test");property.setInternalToken("test-only");return property;
    }
}
