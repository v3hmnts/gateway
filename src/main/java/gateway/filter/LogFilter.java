package gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


public class LogFilter implements GlobalFilter {
    private static final Logger logger = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Loggin pre request. {}", exchange.getRequest().getPath());
        List<String> authHeaderValue = exchange.getRequest().getHeaders().get("Authorization");
        if(authHeaderValue == null){
            return Mono.error(new SecurityException("Requst must contain auth header"));
        }
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            logger.info("Loggin post request");
        }));

    }
}
