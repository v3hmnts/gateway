package gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.configuration.AuthProperties;
import gateway.dto.ErrorResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Order(-1)
public class AuthFilter implements GlobalFilter {
    private final AuthProperties authProperties;
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private ObjectMapper objectMapper;

    @Autowired
    public AuthFilter(AuthProperties authProperties,ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if(authProperties.getExcludedPaths().contains(exchange.getRequest().getPath().toString())){
            return chain.filter(exchange);
        }
        List<String> authHeaderValue = exchange.getRequest().getHeaders().get("Authorization");
        if (authHeaderValue == null || authHeaderValue.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(),"Missing or invalid authorization token",HttpStatus.UNAUTHORIZED,List.of());
            DataBuffer buffer = null;
            try {
                buffer = exchange.getResponse().bufferFactory()
                        .wrap(objectMapper.writeValueAsBytes(errorResponse));
            } catch (JsonProcessingException e) {
                DataBufferUtils.release(buffer);
                return Mono.error(e);
            }
            return exchange
                    .getResponse()
                    .writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }
}
