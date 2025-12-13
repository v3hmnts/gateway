package gateway.handler;

import gateway.client.AuthServiceClient;
import gateway.client.UserServiceClient;
import gateway.dto.UserRegistrationRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RegistrationHandler2 {
    private final UserServiceClient userServiceClient;
    private final AuthServiceClient authServiceClient;

    public RegistrationHandler2(UserServiceClient userServiceClient, AuthServiceClient authServiceClient) {
        this.userServiceClient = userServiceClient;
        this.authServiceClient = authServiceClient;
    }

    public Mono<ServerResponse> processRegistration(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UserRegistrationRequestDto.class)
                .flatMap(request -> authServiceClient.getInternalServiceAuthToken()
                        .flatMap(tokenResponse ->
                                userServiceClient.registerUserInUserService(request, tokenResponse.accessToken())
                                        .flatMap(userServiceResponse -> {
                                            return authServiceClient.registerUserInAuthService(request, userServiceResponse)
                                                    .flatMap(resp -> ServerResponse.status(resp.getStatusCode()).build())
                                                    .onErrorResume(e -> {
                                                        return userServiceClient.compensateUserServiceRegistrationIfFailed(userServiceResponse.getId(), tokenResponse.accessToken())
                                                                .then(Mono.error((e)));
                                                    });
                                        })
                        )
                );

    }


}
