package example.userservice.dto;

import example.userservice.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserRequest(@NotBlank(message = "First name is required") String firstName,
                          @NotBlank(message = "Last name is required") String lastName,
                          @NotBlank(message = "Phone number is required")
                          @Pattern(regexp = "^((8|\\+7)[\\- ]?)?(\\(?\\d{3}\\)?[\\- ]?)?[\\d\\- ]{7,10}$", message = "Invalid phone number")
                          String phoneNumber,
                          @NotNull Long companyId) {
    public User toEntity() {
        return new User(null, firstName, lastName, phoneNumber, companyId);
    }
}
