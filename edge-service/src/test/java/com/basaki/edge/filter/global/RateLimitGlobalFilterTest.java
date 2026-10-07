package com.basaki.edge.filter.global;

import com.basaki.edge.exception.TooManyRequestsException;
import org.junit.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RateLimitGlobalFilterTest {

    @Test
    public void testGetOrder() {
        RateLimitGlobalFilter filter = new RateLimitGlobalFilter(10);
        assertEquals(OrderConstant.FILTER_ORDER_RATE_LIMIT, filter.getOrder());
        assertEquals(10, filter.getRequestsPerMinute());
    }

    @Test
    public void testResolveClientKey() {
        RateLimitGlobalFilter filter = new RateLimitGlobalFilter(10);

        MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/books")
                .remoteAddress(new InetSocketAddress("10.1.2.3", 40000))
                .build();
        assertEquals("10.1.2.3", filter.resolveClientKey(request));

        MockServerHttpRequest noAddress = MockServerHttpRequest.get("http://localhost/books").build();
        assertEquals(RateLimitGlobalFilter.KEY_UNKNOWN, filter.resolveClientKey(noAddress));
    }

    @Test(expected = TooManyRequestsException.class)
    public void testFilterRejectsAboveLimit() {
        RateLimitGlobalFilter filter = new RateLimitGlobalFilter(2);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        for (int i = 0; i < 3; i++) {
            MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/books")
                    .remoteAddress(new InetSocketAddress("10.1.2.3", 40000))
                    .build();
            filter.filter(MockServerWebExchange.from(request), chain);
        }
    }

    @Test
    public void testFilterCountsPerClient() {
        RateLimitGlobalFilter filter = new RateLimitGlobalFilter(1);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest first = MockServerHttpRequest.get("http://localhost/books")
                .remoteAddress(new InetSocketAddress("10.1.2.3", 40000))
                .build();
        MockServerHttpRequest second = MockServerHttpRequest.get("http://localhost/books")
                .remoteAddress(new InetSocketAddress("10.1.2.4", 40000))
                .build();

        filter.filter(MockServerWebExchange.from(first), chain);
        filter.filter(MockServerWebExchange.from(second), chain);

        verify(chain, times(2)).filter(any());
    }
}
