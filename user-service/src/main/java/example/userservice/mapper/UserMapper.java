package example.userservice.mapper;

import example.userservice.dto.UserResponse;
import example.userservice.dto.CompanyResponse;
import example.userservice.dto.UserRequest;
import example.userservice.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequest userRequest) {
        if (userRequest == null) {
            return null;
        }
        return User.builder()
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .phoneNumber(userRequest.getPhoneNumber())
                .companyId(userRequest.getCompanyId())
                .build();
    }

    public UserResponse toResponse(User user, CompanyResponse companyData) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .company(companyData)
                .build();
    }

    public void updateEntity(UserRequest request, User entity) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setCompanyId(request.getCompanyId());
    }
}