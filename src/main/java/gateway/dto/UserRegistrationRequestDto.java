package gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.sql.Date;

public record UserRegistrationRequestDto(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "Name shouldn't be empty")
        @Size(min = 3, max = 100, message = "Name length should be between 3 and 100 characters")
        String name,

        @NotBlank(message = "Surname shouldn't be empty")
        @Size(min = 3, max = 100, message = "Surname length should be between 3 and 100 characters")
        String surname,

        @Past(message = "Birth date should be in past")
        Date birthDate
) {
}
