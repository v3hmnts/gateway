package gateway.configuration;

import gateway.filter.LogFilter;
import gateway.handler.RegistrationHandler;
import gateway.handler.RegistrationHandler2;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class GatewayConfig {

    @Bean
    @Order(-1)  // High priority
    public GlobalFilter loggingFilter() {
        return new LogFilter();
    }

    @Bean
    public RouterFunction<ServerResponse> route(RegistrationHandler2 handler) {
        return RouterFunctions.route()
                .POST("/registration", handler::processRegistration)
                .build();
    }

}
