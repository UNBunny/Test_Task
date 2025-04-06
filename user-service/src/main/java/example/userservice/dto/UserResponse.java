package example.userservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import example.userservice.model.User;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        Map<String, Object> company,
        String companyLoadError
) {
    public static UserResponse fromEntity(User user, Map<String, Object> companyData) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                companyData,
                null
        );
    }

    public static UserResponse withError(User user, String error) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                null,
                error
        );
    }
}