package example.userservice.service;

import example.userservice.client.CompanyClient;
import example.userservice.dto.CompanyResponse;
import example.userservice.dto.UserRequest;
import example.userservice.dto.UserResponse;
import example.userservice.exception.DataProcessingException;
import example.userservice.exception.NotFoundException;
import example.userservice.exception.ValidationException;
import example.userservice.mapper.UserMapper;
import example.userservice.model.User;
import example.userservice.repository.UserRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.ServiceUnavailableException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER_NOT_FOUND = "User not found with id: %s";
    private static final String PHONE_EXISTS = "Phone number already exists";
    private static final String PHONE_UPDATE_FORBIDDEN = "Phone number cannot be updated";
    private static final String COMPANY_NOT_FOUND = "Company not found with id: %s";
    private static final String COMPANY_SERVICE_UNAVAILABLE = "Company service is unavailable";
    private static final String DB_OPERATION_FAILED = "Database operation failed";

    private final UserRepository userRepository;
    private final CompanyClient companyClient;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserRequest userRequest) {
        try {
            validatePhoneNumber(userRequest.getPhoneNumber());
            User user = userMapper.toEntity(userRequest);

            user = userRepository.save(user);
            try {
                associateWithCompany(userRequest, user);
            } catch (ServiceUnavailableException e) {
                log.warn("Created user without company association due to service unavailability");
            }

            CompanyResponse companyData = fetchCompanyData(user.getCompanyId());

            UserResponse response = userMapper.toResponse(user, companyData);

            log.info("Successfully created user with id: {}", user.getId());
            return response;
        } catch (DataAccessException e) {
            log.error("Database error in createUser", e);
            throw new DataProcessingException(DB_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByIdWithCompany(Long id) {
        try {
            User user = getUserById(id);
            CompanyResponse companyData = fetchCompanyData(user.getCompanyId());
            UserResponse userResponse = userMapper.toResponse(user, companyData);
            log.info("Successfully fetched user with id: {}", id);

            return userResponse;
        } catch (DataAccessException e) {
            log.error("Database error in findByIdWithCompany", e);
            throw new DataProcessingException(DB_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        try {
            User existingUser = getUserById(id);
            validatePhoneNumberUpdate(existingUser, userRequest);

            try {
                updateCompanyAssociation(existingUser, userRequest.getCompanyId(), id);
            } catch (ServiceUnavailableException e) {
                log.warn("Updated user without company association change due to service unavailability");
            }

            userMapper.updateEntity(userRequest, existingUser);
            User updatedUser = userRepository.save(existingUser);

            CompanyResponse companyData = fetchCompanyData(updatedUser.getCompanyId());
            UserResponse response = userMapper.toResponse(updatedUser, companyData);

            log.info("Successfully updated user with id: {}", id);
            return response;
        } catch (DataAccessException e) {
            log.error("Database error in updateUser", e);
            throw new DataProcessingException(DB_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        try {
            User user = getUserById(id);
            try {
                removeCompanyAssociation(user);
            } catch (ServiceUnavailableException e) {
                log.warn("Deleted user without removing from company due to service unavailability");
            }
            userRepository.delete(user);
            log.info("Successfully deleted user with id: {}", id);
        } catch (DataAccessException e) {
            log.error("Database error in deleteUser", e);
            throw new DataProcessingException(DB_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        try {
            Page<UserResponse> response = userRepository.findAll(pageable)
                    .map(user -> userMapper.toResponse(user, fetchCompanyData(user.getCompanyId())));
            log.info("Fetched {} of {} users", response.getNumberOfElements(), response.getTotalElements());
            return response;
        } catch (DataAccessException e) {
            log.error("Database error in getAllUsers", e);
            throw new DataProcessingException(DB_OPERATION_FAILED);
        }
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", id);
                    return new NotFoundException(String.format(USER_NOT_FOUND, id));
                });
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            log.error("Phone number already exists: {}", phoneNumber);
            throw new ValidationException(PHONE_EXISTS);
        }
    }

    private void validatePhoneNumberUpdate(User existingUser, UserRequest userRequest) {
        if (!existingUser.getPhoneNumber().equals(userRequest.getPhoneNumber())) {
            log.error("Attempt to update phone number for user id: {}", existingUser.getId());
            throw new ValidationException(PHONE_UPDATE_FORBIDDEN);
        }
    }

    private void associateWithCompany(UserRequest userRequest, User user) throws ServiceUnavailableException {
        if (userRequest.getCompanyId() != null) {
            try {
                CompanyResponse company = companyClient.getCompanyById(userRequest.getCompanyId());
                if (company == null) {
                    throw new NotFoundException(String.format(COMPANY_NOT_FOUND, userRequest.getCompanyId()));
                }
                companyClient.addEmployeeToCompany(userRequest.getCompanyId(), user.getId());
            } catch (FeignException.NotFound e) {
                throw new NotFoundException(String.format(COMPANY_NOT_FOUND, userRequest.getCompanyId()));
            } catch (FeignException e) {
                throw new ServiceUnavailableException(COMPANY_SERVICE_UNAVAILABLE);
            }
        }
    }

    private void removeCompanyAssociation(User user) throws ServiceUnavailableException {
        if (user.getCompanyId() != null) {
            try {
                companyClient.removeEmployeeFromCompany(user.getCompanyId(), user.getId());
            } catch (FeignException e) {
                throw new ServiceUnavailableException(COMPANY_SERVICE_UNAVAILABLE);
            }
        }
    }


    private void updateCompanyAssociation(User user, Long newCompanyId, Long userId) throws ServiceUnavailableException {
        Long currentCompanyId = user.getCompanyId();

        if (!Objects.equals(currentCompanyId, newCompanyId)) {
            try {
                if (currentCompanyId != null) {
                    companyClient.removeEmployeeFromCompany(currentCompanyId, userId);
                }
                if (newCompanyId != null) {
                    CompanyResponse company = companyClient.getCompanyById(newCompanyId);
                    if (company == null) {
                        throw new NotFoundException(String.format(COMPANY_NOT_FOUND, newCompanyId));
                    }
                    companyClient.addEmployeeToCompany(newCompanyId, userId);
                }
                user.setCompanyId(newCompanyId);
            } catch (FeignException.NotFound e) {
                throw new NotFoundException(String.format(COMPANY_NOT_FOUND, newCompanyId));
            } catch (FeignException e) {
                throw new ServiceUnavailableException(COMPANY_SERVICE_UNAVAILABLE);
            }
        }
    }

    private CompanyResponse fetchCompanyData(Long companyId) {
        if (companyId == null) {
            return null;
        }

        try {
            log.debug("Fetching company data for id: {}", companyId);
            return companyClient.getCompanyById(companyId);
        } catch (Exception e) {
            log.error("Failed to fetch company data for id: {}", companyId, e);
            return null;
        }
    }
}