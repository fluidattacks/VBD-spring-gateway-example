package com.basaki.edge.filter.global;

import com.basaki.edge.exception.AuthenticationException;
import com.basaki.edge.exception.BadCredentialsException;
import com.basaki.edge.security.Authenticator;
import com.basaki.edge.security.Credentials;
import com.basaki.edge.security.SecurityAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.basaki.edge.filter.global.OrderConstant.FILTER_ORDER_AUTHENTICATION;

@Component
@Slf4j
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String AUTH_CREDENTIALS = "AUTH_CREDENTIALS";

    public static final String HEADER_FORWARDED_CLIENT_CERT = "x-forwarded-client-cert";

    public static final String NAMESPACE = "gateway-example";

    private SecurityAuthProperties properties;

    @Autowired
    public AuthenticationGlobalFilter(SecurityAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("AuthenticationGlobalFilter - start");

        // Service-to-service calls arrive with the mesh client certificate already
        // validated by the sidecar; workloads from our own namespace do not need to
        // present user credentials again.
        String mtlsHeader = exchange.getRequest().getHeaders().getFirst(HEADER_FORWARDED_CLIENT_CERT);
        if (isIntraNamespaceRequest(mtlsHeader)) {
            log.debug("Intra-namespace request, skipping authentication");
            return chain.filter(exchange);
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            SecurityAuthProperties.Route routeSecurity = properties.getRoutes().get(route.getId());

            Authenticator[] authenticators = routeSecurity.getAuthenticators();
            if (authenticators != null && authenticators.length > 0) {
                boolean success = false;
                for (Authenticator authenticator : authenticators) {
                    if (authenticate(exchange, authenticator)) {
                        success = true;
                        break;
                    }
                }

                if (!success) {
                    throw new AuthenticationException("Authentication failed");
                }
            }
        }

        log.info("AuthenticationGlobalFilter - end");

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER_AUTHENTICATION;
    }

    private boolean isIntraNamespaceRequest(String mtlsHeader) {
        return mtlsHeader != null
                && mtlsHeader.contains("URI=spiffe://cluster.local/ns/" + NAMESPACE + "/");
    }

    private boolean authenticate(ServerWebExchange exchange,
                                 Authenticator authenticator) {
        boolean success = false;
        Credentials credentials;

        try {
            credentials = authenticator.authenticate(exchange.getRequest());
            exchange.getAttributes().put(AUTH_CREDENTIALS, credentials);
            success = true;
        } catch (BadCredentialsException e) {
            log.info(e.getMessage());
        }

        return success;
    }
}
