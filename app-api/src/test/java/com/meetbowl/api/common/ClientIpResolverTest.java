package com.meetbowl.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void resolvesFirstForwardedForIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ApiHeaders.X_FORWARDED_FOR, "198.51.100.10, 10.0.0.5");
        request.addHeader(ApiHeaders.X_REAL_IP, "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("198.51.100.10", ClientIpResolver.resolve(request));
    }

    @Test
    void fallsBackToRealIpWhenForwardedForIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ApiHeaders.X_FORWARDED_FOR, " , ");
        request.addHeader(ApiHeaders.X_REAL_IP, "198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("198.51.100.20", ClientIpResolver.resolve(request));
    }

    @Test
    void fallsBackToRemoteAddrForLocalDevelopment() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");

        assertEquals("::1", ClientIpResolver.resolve(request));
    }
}
