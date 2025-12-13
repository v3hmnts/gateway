package gateway.exception;


import gateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ControllerAdvice {


    @ExceptionHandler(UserRegistrationException.class)
    public Mono<ResponseEntity<ErrorResponse>> hangleRegistrationException(UserRegistrationException ex) {
        return Mono.just(new ResponseEntity<>(ex.getErrorResponse(), ex.getErrorResponse().status()));
    }

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

    @ExceptionHandler(TokenValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTokenValidationException(TokenValidationException ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                null
        );

        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler({ItemNotFoundException.class, OrderNotFoundException.class, UserNotFoundException.class})
    public Mono<ResponseEntity<ErrorResponse>> handleBaseException(RuntimeException ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                null
        );
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleHandlerMethodValidationException(HandlerMethodValidationException ex, ServerWebExchange exchange) {

        List<String> errors = new ArrayList<>();

        ex.getParameterValidationResults().forEach(parameterResult -> {
            String parameterName = parameterResult.getMethodParameter().getParameterName();

            parameterResult.getResolvableErrors().forEach(error -> {
                errors.add(String.format("%s:%s", parameterName, error.getDefaultMessage()));
            });
        });

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                errors
        );

        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Mono<ResponseEntity<ErrorResponse>> handleValidationExceptions(MethodArgumentNotValidException ex, ServerWebExchange exchange) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                errors
        );

        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR));
    }

}
