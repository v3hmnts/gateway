package gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.configuration.AuthProperties;
import gateway.dto.ErrorResponse;
import gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
@Order(-1)
public class AuthFilter implements GlobalFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final AuthProperties authProperties;
    private ObjectMapper objectMapper;
    private JwtUtil jwtUtil;

    @Autowired
    public AuthFilter(AuthProperties authProperties, ObjectMapper objectMapper, JwtUtil jwtUtil) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (authProperties.getExcludedPaths().contains(exchange.getRequest().getPath().toString())) {
            return chain.filter(exchange);
        }
        List<String> authHeaderValue = exchange.getRequest().getHeaders().get(AUTH_HEADER);
        if (authHeaderValue == null || authHeaderValue.isEmpty()) {
            return generateMonoErrorResponse(exchange,"Missing or invalid authorization token");
        }
        String jwt = extractJwtFromRequest(authHeaderValue.getFirst());
        if(jwt == null || !jwtUtil.validateToken(jwt)){
            return generateMonoErrorResponse(exchange,"Token is not valid");
        }
        return chain.filter(exchange);
    }
    private String extractJwtFromRequest(String authHeaderValue) {
        if (StringUtils.hasText(authHeaderValue) && authHeaderValue.startsWith(BEARER_PREFIX)) {
            return authHeaderValue.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> generateMonoErrorResponse(ServerWebExchange exchange, String message){
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), message, HttpStatus.UNAUTHORIZED, List.of());

        try {
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(objectMapper.writeValueAsBytes(errorResponse));
            return exchange
                    .getResponse()
                    .writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }
}
