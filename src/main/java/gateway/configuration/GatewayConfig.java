package gateway.configuration;

import gateway.filter.AuthFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class GatewayConfig {

    @Bean
    @Order(-1)  // High priority
    public GlobalFilter loggingFilter() {
        return new AuthFilter();
    }

}
