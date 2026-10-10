package com.vita.web.xss;

import jakarta.servlet.ReadListener;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class XssRequestWrapperTest {
    @Test
    void partialAndEmptyReadsHaveCorrectFinishedState() throws Exception {
        var wrapper = new XssRequestWrapper(new MockHttpServletRequest(),new byte[]{1,2});
        var input = wrapper.getInputStream();
        assertTrue(input.isReady()); assertFalse(input.isFinished()); assertEquals(2,input.available());
        assertEquals(1,input.read()); assertFalse(input.isFinished()); assertEquals(2,input.read());
        assertTrue(input.isFinished()); assertEquals(-1,input.read());
        assertArrayEquals(new byte[]{1,2},wrapper.getInputStream().readAllBytes());
        assertTrue(new XssRequestWrapper(new MockHttpServletRequest(),new byte[0]).getInputStream().isFinished());
    }

    @Test
    void readerUsesDeclaredCharsetWithoutChangingBytes() throws Exception {
        var request = new MockHttpServletRequest(); request.setCharacterEncoding("UTF-16LE");
        var bytes = "中文 & < 5".getBytes(StandardCharsets.UTF_16LE);
        var wrapper = new XssRequestWrapper(request,bytes);
        assertEquals("中文 & < 5",wrapper.getReader().readLine());
        assertArrayEquals(bytes,wrapper.getInputStream().readAllBytes());
    }

    @Test
    void asyncReadListenerSeesOriginalDataThenCompletion() throws Exception {
        var request = new MockHttpServletRequest(); request.setAsyncSupported(true); request.startAsync();
        var stream = new XssRequestWrapper(request,new byte[]{3,4}).getInputStream();
        var finished = new AtomicBoolean();
        ReadListener listener = new ReadListener() {
            @Override public void onDataAvailable() throws IOException { assertArrayEquals(new byte[]{3,4},stream.readAllBytes()); }
            @Override public void onAllDataRead() { finished.set(true); }
            @Override public void onError(Throwable failure) { fail(failure); }
        };
        stream.setReadListener(listener);
        assertTrue(finished.get());
        assertThrows(IllegalStateException.class,() -> stream.setReadListener(listener));
        var sync = new XssRequestWrapper(new MockHttpServletRequest(),new byte[0]).getInputStream();
        assertThrows(IllegalStateException.class,() -> sync.setReadListener(listener));
    }
}
