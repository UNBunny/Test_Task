package example.userservice.service;

import example.userservice.dto.UserRequest;
import example.userservice.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserRequest userRequest);
    UserResponse findByIdWithCompany(Long id);
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUser(Long id, UserRequest userRequest);
    void deleteUser(Long id);
}
