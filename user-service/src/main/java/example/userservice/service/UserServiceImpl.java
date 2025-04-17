package example.userservice.service;

import example.userservice.client.CompanyClient;
import example.userservice.dto.CompanyResponse;
import example.userservice.dto.UserRequest;
import example.userservice.dto.UserResponse;
import example.userservice.exception.*;
import example.userservice.mapper.UserMapper;
import example.userservice.model.User;
import example.userservice.repository.UserRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CompanyClient companyClient;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserRequest userRequest) {
        validateUserRequest(userRequest);
        validatePhoneNumber(userRequest.getPhoneNumber());

        User user = userMapper.toEntity(userRequest);
        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserCreatedEvent(user.getId(), user.getCompanyId()));

        UserResponse response = userMapper.toResponse(user, fetchCompanyData(user.getCompanyId()));
        log.info("Successfully created user with ID: {}", user.getId());
        return response;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        try {
            companyClient.addEmployeeToCompany(event.companyId(), event.userId());
            log.info("User {} associated with company {}", event.userId(), event.companyId());
        } catch (FeignException.NotFound e) {
            log.error("Company not found for user {} and company {}", event.userId(), event.companyId(), e);
        } catch (FeignException e) {
            log.error("Failed to associate user {} with company {}", event.userId(), event.companyId(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByIdWithCompany(Long id) {
        User user = getUserById(id);
        CompanyResponse companyData = fetchCompanyData(user.getCompanyId());
        UserResponse userResponse = userMapper.toResponse(user, companyData);
        log.info("Successfully fetched user with id: {}", id);
        return userResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User existingUser = getUserById(id);
        validatePhoneNumberUpdate(existingUser, userRequest);

        updateCompanyAssociation(existingUser, userRequest.getCompanyId(), id);
        userMapper.updateEntity(userRequest, existingUser);
        User updatedUser = userRepository.save(existingUser);

        CompanyResponse companyData = fetchCompanyData(updatedUser.getCompanyId());
        UserResponse response = userMapper.toResponse(updatedUser, companyData);
        log.info("Successfully updated user with id: {}", id);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = getUserById(id);
        removeCompanyAssociation(user);
        userRepository.delete(user);
        log.info("Successfully deleted user with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UserResponse> getUsersByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = StreamSupport.stream(userRepository.findAllById(ids).spliterator(), false)
                .toList();

        Map<Long, UserResponse> userResponses = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> userMapper.toResponse(user, null)
                ));
        log.info("Successfully fetched users by IDs: {}", ids);
        return userResponses;
    }

    @Override
    @Transactional
    public void addCompanyToUser(Long employeeId, Long companyId) {
        User user = getUserById(employeeId);
        user.setCompanyId(companyId);
        userRepository.save(user);

        try {
            companyClient.addEmployeeToCompany(companyId, employeeId);
            log.info("Successfully added user {} to company {}", employeeId, companyId);
        } catch (FeignException.NotFound e) {
            log.error("Company not found with id: {}", companyId, e);
            throw new NotFoundException(String.format("Company not found with id: %s", companyId));
        } catch (FeignException e) {
            log.error("Failed to add user {} to company {}", employeeId, companyId, e);
            throw new ServiceUnavailableException("Company service is unavailable");
        }
    }

    @Override
    public Boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<UserResponse> response = userRepository.findAll(pageable)
                .map(user -> userMapper.toResponse(user, fetchCompanyData(user.getCompanyId())));
        log.info("Successfully fetched {} of {} users", response.getNumberOfElements(), response.getTotalElements());
        return response;
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User not found with id: %s", id)));
    }

    private void validateUserRequest(UserRequest userRequest) {
        if (userRequest.getCompanyId() == null) {
            throw new ValidationException("Company ID is required for user creation");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ValidationException("Phone number already exists");
        }
    }

    private void validatePhoneNumberUpdate(User existingUser, UserRequest userRequest) {
        if (!existingUser.getPhoneNumber().equals(userRequest.getPhoneNumber())) {
            throw new ValidationException("Phone number cannot be updated");
        }
    }

    private void updateCompanyAssociation(User user, Long newCompanyId, Long userId) {
        Long currentCompanyId = user.getCompanyId();

        if (!Objects.equals(currentCompanyId, newCompanyId)) {
            try {
                if (currentCompanyId != null) {
                    companyClient.removeEmployeeFromCompany(currentCompanyId, userId);
                }
                if (newCompanyId != null) {
                    CompanyResponse company = companyClient.getCompanyById(newCompanyId);
                    if (company == null) {
                        throw new NotFoundException(String.format("Company not found with id: %s", newCompanyId));
                    }
                    companyClient.addEmployeeToCompany(newCompanyId, userId);
                }
                user.setCompanyId(newCompanyId);
            } catch (FeignException.NotFound e) {
                throw new NotFoundException(String.format("Company not found with id: %s", newCompanyId));
            } catch (FeignException e) {
                throw new ServiceUnavailableException("Company service is unavailable");
            }
        }
    }

    private void removeCompanyAssociation(User user) {
        if (user.getCompanyId() != null) {
            try {
                companyClient.removeEmployeeFromCompany(user.getCompanyId(), user.getId());
            } catch (FeignException e) {
                throw new ServiceUnavailableException("Company service is unavailable");
            }
        }
    }

    private CompanyResponse fetchCompanyData(Long companyId) {
        if (companyId == null) {
            return null;
        }

        try {
            return companyClient.getCompanyById(companyId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(String.format("Company not found with id: %s", companyId));
        } catch (FeignException e) {
            throw new ServiceUnavailableException("Company service is unavailable");
        }
    }

    public record UserCreatedEvent(Long userId, Long companyId) {
    }
}