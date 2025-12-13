package gateway.client;

import gateway.dto.ErrorResponse;
import gateway.dto.UserRegistrationRequestDto;
import gateway.dto.UserServiceUserRegistrationRequestDto;
import gateway.dto.UserServiceUserRegistrationResponseDto;
import gateway.exception.UserRegistrationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
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
public class UserServiceClient {

    @Value("${USER_SERVICE_BASE_URL:http://localhost:8080}")
    private String USER_SERVICE_BASE_URL;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    @Value("${user.service.timeout.seconds:2}")
    private int userServiceDefaultTimeOut;
    private WebClient userServiceWebClient;
    private WebClient.Builder webClientBuilder;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @Autowired
    public UserServiceClient(WebClient.Builder webClientBuilder,CircuitBreakerRegistry circuitBreakerRegistry,RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void init() {
        this.userServiceWebClient = webClientBuilder.baseUrl(USER_SERVICE_BASE_URL).build();
    }

    public Mono<ResponseEntity<Void>> compensateUserServiceRegistrationIfFailed(Long userId, String token) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        Retry retry = retryRegistry.retry("userService");
        return userServiceWebClient.delete()
                .uri("/api/v1/users/{userId}", userId)
                .header("Authorization", "Bearer " + token)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.toBodilessEntity();
                    }
                    return response.bodyToMono(ErrorResponse.class).flatMap(errorResponse -> Mono.error(new UserRegistrationException(errorResponse)));
                })
                .timeout(Duration.ofSeconds(userServiceDefaultTimeOut))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));


    }

    public Mono<UserServiceUserRegistrationResponseDto> registerUserInUserService(UserRegistrationRequestDto original, String token) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        Retry retry = retryRegistry.retry("userService");
        UserServiceUserRegistrationRequestDto userRequest =
                new UserServiceUserRegistrationRequestDto(original.username(), original.surname(), original.birthDate(), original.email());

        return userServiceWebClient.post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + token)
                .bodyValue(userRequest)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(UserServiceUserRegistrationResponseDto.class);
                    }
                    return response.bodyToMono(ErrorResponse.class).flatMap(errorResponse -> Mono.error(new UserRegistrationException(errorResponse)));
                })
                .timeout(Duration.ofSeconds(userServiceDefaultTimeOut))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}
