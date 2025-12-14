package gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


public class AuthFilter implements GlobalFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<String> authHeaderValue = exchange.getRequest().getHeaders().get("Authorization");
        if (authHeaderValue == null) {
            return Mono.error(new SecurityException(String.format("Request for %s endpoint requires authentification", exchange.getRequest().getPath())));
        }
        return chain.filter(exchange);
    }
}
