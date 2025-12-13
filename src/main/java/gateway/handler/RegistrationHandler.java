package gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.client.AuthServiceClient;
import gateway.dto.*;
import gateway.exception.UserRegistrationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RegistrationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationHandler.class);
    @Value("${AUTH_SERVICE_BASE_URL:http://localhost:8080}")
    private String AUTH_SERVICE_BASE_URL;
    private final WebClient.Builder webClientBuilder;
    @Value("${USER_SERVICE_BASE_URL:http://localhost:8080}")
    private String USER_SERVICE_BASE_URL;
    @Value("${internal.service.api.key}")
    private String INTERNAL_SERVICE_API_KEY;
    private WebClient authClient;
    private WebClient userClient;
    private final ObjectMapper objectMapper;

    public RegistrationHandler(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.authClient = webClientBuilder.baseUrl(AUTH_SERVICE_BASE_URL).build();
        this.userClient = webClientBuilder.baseUrl(USER_SERVICE_BASE_URL).build();
    }

    public Mono<ServerResponse> handleRegistration(ServerRequest request) {
        return request.bodyToMono(UserRegistrationRequestDto.class)
                .flatMap(originalRequest -> {
                    return getServiceToken()
                            .flatMap(serverTokenAuthResponse -> {
                                return registerWithUserService(originalRequest, serverTokenAuthResponse.accessToken())
                                        .flatMap(resp -> {
                                            if (resp.getStatusCode().is2xxSuccessful()) {
                                                UserServiceUserRegistrationResponseDto userServiceUserRegistrationResponseDto = objectMapper.convertValue(resp.getBody(), UserServiceUserRegistrationResponseDto.class);
                                                UserAuthServiceRegistrationDto authRequest = new UserAuthServiceRegistrationDto(userServiceUserRegistrationResponseDto.getId(), originalRequest.username(), originalRequest.password(), originalRequest.email());
                                                return registerWithAuthService(authRequest)
                                                        .flatMap(authResponse -> {
                                                            logger.info(authResponse.getStatusCode().toString());
                                                            if (authResponse.getStatusCode().is2xxSuccessful()) {
                                                                // Success - both services succeeded
                                                                return ServerResponse.ok().build();
                                                            } else {
                                                                // Auth failed - COMPENSATE by deleting user
                                                                logger.warn("Auth registration failed, compensating by deleting user: {}", userServiceUserRegistrationResponseDto.getId());
                                                                return deleteUserById(userServiceUserRegistrationResponseDto.getId(), serverTokenAuthResponse.accessToken())
                                                                        .then(ServerResponse.status(authResponse.getStatusCode()).build());
                                                            }
                                                        });

                                            } else {
                                                return ServerResponse.status(resp.getStatusCode())
                                                        .headers(httpHeaders -> httpHeaders.addAll(resp.getHeaders()))
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .bodyValue(resp.getBody());
                                            }

                                        });
                            });

                });
    }

    private Mono<ResponseEntity<?>> registerWithAuthService(UserAuthServiceRegistrationDto authRequest) {

        return authClient.post()
                .uri("/api/auth/register")
                .bodyValue(authRequest)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(new ParameterizedTypeReference<ResponseEntity<?>>() {
                        });
                    }
                    return response.bodyToMono(ErrorResponse.class).flatMap(errorResponse -> Mono.error(new UserRegistrationException(errorResponse)));
                });

    }

    private Mono<ResponseEntity<?>> deleteUserById(Long userId, String token) {

        return userClient.delete()
                .uri("/api/v1/users/{userId}", userId)
                .header("Authorization", "Bearer " + token)
                .exchangeToMono(resp -> {
                    HttpStatus status = (HttpStatus) resp.statusCode();
                    HttpHeaders headers = new HttpHeaders();
                    headers.addAll(resp.headers().asHttpHeaders());

                    if (status.is2xxSuccessful()) {
                        return resp.bodyToMono(new ParameterizedTypeReference<ResponseEntity<?>>() {
                        });
                    } else {
                        // Propagate error response exactly as received
                        return resp.bodyToMono(ErrorResponse.class)
                                .map(errorBody -> ResponseEntity.status(status)
                                        .headers(headers)
                                        .body(errorBody));
                    }
                });

    }

    private Mono<ServerTokenAuthResponse> getServiceToken() {

        return authClient.post()
                .uri("/api/auth/internal/token")
                .header("x-api-key", INTERNAL_SERVICE_API_KEY)
                .retrieve()
                .bodyToMono(ServerTokenAuthResponse.class);

    }

    private Mono<ResponseEntity<?>> registerWithUserService(UserRegistrationRequestDto original, String token) {
        UserServiceUserRegistrationRequestDto userRequest =
                new UserServiceUserRegistrationRequestDto(original.username(), original.surname(), original.birthDate(), original.email());

        return userClient.post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + token)
                .bodyValue(userRequest)
                .exchangeToMono(response -> {
                    HttpStatus status = (HttpStatus) response.statusCode();
                    HttpHeaders headers = new HttpHeaders();
                    headers.addAll(response.headers().asHttpHeaders());

                    if (status.is2xxSuccessful()) {
                        return response.bodyToMono(UserServiceUserRegistrationResponseDto.class)
                                .map(body -> ResponseEntity.status(status)
                                        .headers(headers)
                                        .body(body));
                    } else {
                        // Propagate error response exactly as received
                        return response.bodyToMono(ErrorResponse.class)
                                .map(errorBody -> ResponseEntity.status(status)
                                        .headers(headers)
                                        .body(errorBody));
                    }
                });
    }
}
