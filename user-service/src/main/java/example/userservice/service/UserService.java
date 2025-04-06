package example.userservice.service;


import example.userservice.client.CompanyClient;
import example.userservice.dto.UserRequest;
import example.userservice.dto.UserResponse;
import example.userservice.model.User;
import example.userservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final CompanyClient companyClient;

    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByPhoneNumber(userRequest.phoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        User user = userRepository.save(userRequest.toEntity());
        log.info("User created: {}", user);
        return UserResponse.fromEntity(user, null);
    }

    public UserResponse findByIdWithCompany(Long id) {
        log.info("Fetching user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        log.info("Found user: {}", user);

        if (user.getCompanyId() == null) {
            log.info("User has no company assigned");
            return UserResponse.fromEntity(user, null);
        }

        try {
            log.info("Fetching company data for companyId: {}", user.getCompanyId());
            Map<String, Object> companyData = companyClient.getCompanyRawData(user.getCompanyId());
            log.info("Received company data: {}", companyData);
            return UserResponse.fromEntity(user, companyData);
        } catch (Exception e) {
            log.error("Failed to fetch company data for companyId: " + user.getCompanyId(), e);
            return UserResponse.withError(user, "Failed to load company data: " + e.getMessage());
        }
    }

    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User existingUser = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!existingUser.getPhoneNumber().equals(userRequest.phoneNumber())) {
            throw new IllegalArgumentException("Phone number cannot be updated");
        }
        existingUser.setFirstName(userRequest.phoneNumber());
        existingUser.setLastName(userRequest.lastName());
        existingUser.setCompanyId(userRequest.companyId());

        User updUser = userRepository.save(existingUser);
        return UserResponse.fromEntity(updUser, null);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public List<UserResponse> getAllUsers() {
        try {
            log.info("Fetching all users from database");
            List<User> users = (List<User>) userRepository.findAll();

            if (users.isEmpty()) {
                log.warn("No users found in the database");
                return List.of();
            }

            return users.stream()
                    .map(user -> {
                        try {
                            Map<String, Object> companyData = companyClient.getCompanyRawData(user.getCompanyId());
                            return UserResponse.fromEntity(user, companyData);
                        } catch (Exception e) {
                            log.error("Failed to load company data for user ID: {}", user.getId(), e);
                            return UserResponse.withError(user, "Company data unavailable");
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            throw new RuntimeException("Failed to retrieve users", e);
        }
    }

    public Map<String, Object> getUserRawData(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        return Map.of("id", user.getId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "phoneNumber", user.getPhoneNumber());
    }
}
