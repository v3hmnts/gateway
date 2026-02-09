package gateway.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        String message,
        HttpStatus status,
        List<String> details
) {
}