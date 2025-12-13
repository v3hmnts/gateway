package gateway.exception;

import gateway.dto.ErrorResponse;
import lombok.Getter;

public class UserRegistrationException extends RuntimeException {
    @Getter
    private final ErrorResponse errorResponse;

    public UserRegistrationException(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
