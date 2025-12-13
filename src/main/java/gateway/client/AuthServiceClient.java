package gateway.client;

import gateway.dto.*;

import gateway.exception.UserRegistrationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@NoArgsConstructor
@Component
public class AuthServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceClient.class);

    @Value("${AUTH_SERVICE_BASE_URL:http://localhost:9090}")
    private String AUTH_SERVICE_BASE_URL;

    @Value("${internal.service.api.key}")
    private String INTERNAL_SERVICE_API_KEY;

    @Value("${auth.service.timeout.seconds:2}")
    private int authServiceDefaultTimeOut;
    private WebClient authServiceWebClient;
    private WebClient.Builder webClientBuilder;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @Autowired
    public AuthServiceClient(WebClient.Builder webClientBuilder,CircuitBreakerRegistry circuitBreakerRegistry,RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void init() {
        this.authServiceWebClient = webClientBuilder.baseUrl(AUTH_SERVICE_BASE_URL).build();
    }


    public Mono<ServerTokenAuthResponse> getInternalServiceAuthToken() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        Retry retry = retryRegistry.retry("authService");
        return authServiceWebClient.post()
                .uri("/api/v1/auth/internal/token")
                .header("x-api-key", INTERNAL_SERVICE_API_KEY)
                .retrieve()
                .bodyToMono(ServerTokenAuthResponse.class)
                .timeout(Duration.ofSeconds(authServiceDefaultTimeOut))
                .doOnError(e->logger.error("Failed to obtain service auth token. {}",e.getMessage()))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));

    }

    public Mono<ResponseEntity<Void>> registerUserInAuthService(UserRegistrationRequestDto userRegistrationRequestDto, UserServiceUserRegistrationResponseDto userRegistrationResponseDto) {
        UserAuthServiceRegistrationDto authServiceRegistrationDto = new UserAuthServiceRegistrationDto(
                userRegistrationResponseDto.getId(),
                userRegistrationRequestDto.username(),
                userRegistrationRequestDto.password(),
                userRegistrationRequestDto.email()
        );
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        Retry retry = retryRegistry.retry("authService");

        return authServiceWebClient.post()
                .uri("/api/v1/auth/register")
                .bodyValue(authServiceRegistrationDto)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.toBodilessEntity();
                    }
                    return response.bodyToMono(ErrorResponse.class).flatMap(errorResponse -> Mono.error(new UserRegistrationException(errorResponse)));
                })
                .timeout(Duration.ofSeconds(authServiceDefaultTimeOut))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}
