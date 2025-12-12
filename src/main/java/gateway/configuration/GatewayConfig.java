package gateway.configuration;

import gateway.handler.RegistrationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class GatewayConfig {

        @Bean
        public RouterFunction<ServerResponse> route(RegistrationHandler handler) {
            return RouterFunctions.route()
                    .POST("/registration", handler::handleRegistration)
                    .build();
        }

}
