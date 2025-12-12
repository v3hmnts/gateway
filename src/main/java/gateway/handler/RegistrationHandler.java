package gateway.handler;

import gateway.dto.UserAuthServiceRegistrationDto;
import gateway.dto.UserRegistrationRequestDto;
import gateway.dto.UserServiceUserRegistrationResponseDto;
import gateway.dto.UserUserServiceRegistrationDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class RegistrationHandler {

    @Value("${USER_SERVICE_BASE_URL:http://localhost:9090}")
    private String AUTH_SERVICE_BASE_URL;
    @Value("${USER_SERVICE_BASE_URL:http://localhost:8080}")
    private String USER_SERVICE_BASE_URL;
    private WebClient authClient;
    private WebClient userClient;
    private final WebClient.Builder webClientBuilder;

    public RegistrationHandler(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        this.authClient = webClientBuilder.baseUrl(AUTH_SERVICE_BASE_URL).build();
        this.userClient = webClientBuilder.baseUrl(USER_SERVICE_BASE_URL).build();
    }

    public Mono<ServerResponse> handleRegistration(ServerRequest request) {
        return request.bodyToMono(UserRegistrationRequestDto.class)
                .flatMap(originalRequest ->
                    // Split the request
                    registerWithUserService(originalRequest)
                            .flatMap(resp -> {
                                UserAuthServiceRegistrationDto authRequest = new UserAuthServiceRegistrationDto(resp.getId(), originalRequest.username(), originalRequest.password(), originalRequest.email());
                                return registerWithAuthService(authRequest).thenReturn(resp);
                            })
                            .then(
                                    ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(Map.of("status", "success"))
                            )
                )
                .onErrorResume(e -> ServerResponse.badRequest()
                        .bodyValue(Map.of("error", e.getMessage())));
    }

    private Mono<Void> registerWithAuthService(UserAuthServiceRegistrationDto authRequest) {

        return authClient.post()
                .uri("/api/auth/register")
                .bodyValue(authRequest)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    private Mono<UserServiceUserRegistrationResponseDto> registerWithUserService(UserRegistrationRequestDto original) {
        UserUserServiceRegistrationDto userRequest =
                new UserUserServiceRegistrationDto(original.username(), original.surname(),original.birthDate(),original.email());

        return userClient.post()
                .uri("/api/v1/users")
                .bodyValue(userRequest)
                .retrieve()
                .bodyToMono(UserServiceUserRegistrationResponseDto.class);
    }
}
