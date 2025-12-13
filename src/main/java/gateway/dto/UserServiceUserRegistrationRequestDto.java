package gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.sql.Date;

public record UserServiceUserRegistrationRequestDto(
        @NotBlank(message = "Name shouldn't be empty")
        @Size(min = 3, max = 100, message = "Name length should be between 3 and 100 characters")
        String name,

        @NotBlank(message = "Surname shouldn't be empty")
        @Size(min = 3, max = 100, message = "Surname length should be between 3 and 100 characters")
        String surname,

        @Past(message = "Birth date should be in past")
        Date birthDate,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email
) {
}
