package gateway.dto;

public record ServerTokenAuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn
) {
}
