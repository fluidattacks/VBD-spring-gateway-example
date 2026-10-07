package com.basaki.edge.filter.global;

import com.basaki.edge.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.basaki.edge.filter.global.OrderConstant.FILTER_ORDER_RATE_LIMIT;

/**
 * Fixed-window request limiter that runs before authentication so that
 * credential guessing against the edge is throttled per client.
 */
@Component
@Slf4j
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    public static final String KEY_UNKNOWN = "unknown";

    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private static final long WINDOW_MILLIS = 60_000L;

    private final int requestsPerMinute;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitGlobalFilter(
            @Value("${security.rate-limit.requests-per-minute:120}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = resolveClientKey(exchange.getRequest());
        long currentWindow = System.currentTimeMillis() / WINDOW_MILLIS;

        Window window = windows.compute(key,
                (k, w) -> (w == null || w.id != currentWindow) ? new Window(currentWindow) : w);

        int count = window.count.incrementAndGet();
        if (count > requestsPerMinute) {
            log.warn("Rate limit exceeded for client {} ({} requests)", key, count);
            throw new TooManyRequestsException("Too many requests");
        }

        return chain.filter(exchange);
    }

    String resolveClientKey(ServerHttpRequest request) {
        // When fronted by the ingress the socket peer is the load balancer, so
        // prefer the originating client address it forwards.
        String forwardedFor = request.getHeaders().getFirst(HEADER_FORWARDED_FOR);
        if (StringUtils.isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remote = request.getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }

        return KEY_UNKNOWN;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER_RATE_LIMIT;
    }

    private static final class Window {
        private final long id;

        private final AtomicInteger count = new AtomicInteger();

        private Window(long id) {
            this.id = id;
        }
    }
}
