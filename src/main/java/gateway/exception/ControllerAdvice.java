package gateway.exception;


import gateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.AccessDeniedException;
import java.time.Instant;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAccessDeniedException(AccessDeniedException ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                "Access denied",
                HttpStatus.FORBIDDEN,
                null
        );

        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN));
    }


    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, null);
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR));
    }

}
