package example.userservice;

import example.userservice.client.CompanyClient;
import example.userservice.dto.CompanyResponse;
import example.userservice.dto.UserRequest;
import example.userservice.dto.UserResponse;
import example.userservice.exception.NotFoundException;
import example.userservice.exception.ServiceUnavailableException;
import example.userservice.exception.ValidationException;
import example.userservice.mapper.UserMapper;
import example.userservice.model.User;
import example.userservice.repository.UserRepository;
import example.userservice.service.UserServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);
    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyClient companyClient;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateUser_Success() {
        UserRequest userRequest = new UserRequest("Ivan", "Ivanov", "+79021111111", 1L);
        User user = new User();
        user.setId(1L);
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setPhoneNumber("+79021111111");
        user.setCompanyId(1L);
        CompanyResponse companyResponse = new CompanyResponse(1L, "TestCompany", 100000L);
        UserResponse userResponse = new UserResponse(1L, "Ivan", "Ivanov", "+79021111111", companyResponse);

        when(userRepository.existsByPhoneNumber("+79021111111")).thenReturn(false);
        when(userMapper.toEntity(userRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(companyClient.getCompanyById(1L)).thenReturn(companyResponse);
        when(userMapper.toResponse(user, companyResponse)).thenReturn(userResponse);

        UserResponse result = userService.createUser(userRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Ivan", result.getFirstName());
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(UserServiceImpl.UserCreatedEvent.class));
    }

    @Test
    public void testCreateUser_PhoneNumberExists() {
        UserRequest userRequest = new UserRequest("Ivan", "Ivanov", "+79021111111", 1L);
        when(userRepository.existsByPhoneNumber("+79021111111")).thenReturn(true);

        assertThrows(ValidationException.class, () -> userService.createUser(userRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testCreateUser_CompanyIdNull() {
        UserRequest userRequest = new UserRequest("Ivan", "Ivanov", "+79021111111", null);

        assertThrows(ValidationException.class, () -> userService.createUser(userRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testFindByIdWithCompany_Success() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setCompanyId(1L);
        CompanyResponse companyResponse = new CompanyResponse(1L, "TestCompany", 100000L);
        UserResponse userResponse = new UserResponse(1L, "Ivan", "Ivanov", "+79021111111", companyResponse);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(companyClient.getCompanyById(1L)).thenReturn(companyResponse);
        when(userMapper.toResponse(user, companyResponse)).thenReturn(userResponse);

        UserResponse result = userService.findByIdWithCompany(id);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(companyResponse, result.getCompany());
    }

    @Test
    public void testFindByIdWithCompany_UserNotFound() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.findByIdWithCompany(id));
    }

    @Test
    public void testFindByIdWithCompany_CompanyServiceUnavailable() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setCompanyId(1L);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(companyClient.getCompanyById(1L)).thenThrow(FeignException.class);

        assertThrows(ServiceUnavailableException.class, () -> userService.findByIdWithCompany(id));
    }

    @Test
    public void testUpdateUser_Success() {
        Long id = 1L;
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setFirstName("OldName");
        existingUser.setLastName("OldLastName");
        existingUser.setPhoneNumber("+79021111111");
        existingUser.setCompanyId(1L);

        UserRequest userRequest = new UserRequest("Ivan", "Ivanov", "+79021111111", 2L);
        CompanyResponse companyResponse = new CompanyResponse(2L, "Company2", 200000L);
        UserResponse expectedResponse = new UserResponse(id, "Ivan", "Ivanov", "+79021111111", companyResponse);

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));

        when(companyClient.getCompanyById(2L)).thenReturn(companyResponse);
        doNothing().when(companyClient).removeEmployeeFromCompany(1L, id);
        doNothing().when(companyClient).addEmployeeToCompany(2L, id);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser, companyResponse)).thenReturn(expectedResponse);

        UserResponse result = userService.updateUser(id, userRequest);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        verify(userRepository).findById(id);

        verify(companyClient, times(2)).getCompanyById(2L);
        verify(companyClient).removeEmployeeFromCompany(1L, id);
        verify(companyClient).addEmployeeToCompany(2L, id);
        verify(userRepository).save(existingUser);
        verify(userMapper).toResponse(existingUser, companyResponse);
    }

    @Test
    public void testUpdateUser_PhoneNumberChanged() {
        Long id = 1L;
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setPhoneNumber("+79021111111");
        UserRequest userRequest = new UserRequest("Ivan", "Ivanov", "0987654321", 1L);

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));

        assertThrows(ValidationException.class, () -> userService.updateUser(id, userRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testUpdateUser_UserNotFound() {
        Long id = 1L;
        UserRequest userRequest = new UserRequest("Ivan", "Ivanov", "+79021111111", 1L);

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateUser(id, userRequest));
    }

    @Test
    public void testDeleteUser_Success() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setCompanyId(1L);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doNothing().when(companyClient).removeEmployeeFromCompany(1L, id);
        doNothing().when(userRepository).delete(user);

        userService.deleteUser(id);

        verify(userRepository).delete(user);
        verify(companyClient).removeEmployeeFromCompany(1L, id);
    }

    @Test
    public void testDeleteUser_UserNotFound() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.deleteUser(id));
    }

    @Test
    public void testGetUsersByIds_Success() {
        Set<Long> ids = Set.of(1L, 2L);
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        UserResponse response1 = new UserResponse(1L, "Ivan", "Ivanov", "+79021111111", null);
        UserResponse response2 = new UserResponse(2L, "Jane", "Smith", "0987654321", null);

        when(userRepository.findAllById(ids)).thenReturn(Arrays.asList(user1, user2));
        when(userMapper.toResponse(user1, null)).thenReturn(response1);
        when(userMapper.toResponse(user2, null)).thenReturn(response2);

        Map<Long, UserResponse> result = userService.getUsersByIds(ids);

        assertEquals(2, result.size());
        assertEquals("Ivan", result.get(1L).getFirstName());
        assertEquals("Jane", result.get(2L).getFirstName());
    }

    @Test
    public void testGetUsersByIds_EmptySet() {
        Set<Long> ids = Collections.emptySet();

        Map<Long, UserResponse> result = userService.getUsersByIds(ids);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAddCompanyToUser_Success() {
        Long employeeId = 1L;
        Long companyId = 1L;
        User user = new User();
        user.setId(employeeId);

        when(userRepository.findById(employeeId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        doNothing().when(companyClient).addEmployeeToCompany(companyId, employeeId);

        userService.addCompanyToUser(employeeId, companyId);

        assertEquals(companyId, user.getCompanyId());
        verify(userRepository).save(user);
        verify(companyClient).addEmployeeToCompany(companyId, employeeId);
    }

    @Test
    public void testAddCompanyToUser_CompanyNotFound() {
        Long employeeId = 1L;
        Long companyId = 1L;
        User user = new User();
        user.setId(employeeId);

        when(userRepository.findById(employeeId)).thenReturn(Optional.of(user));
        doThrow(FeignException.NotFound.class).when(companyClient).addEmployeeToCompany(companyId, employeeId);

        assertThrows(NotFoundException.class, () -> userService.addCompanyToUser(employeeId, companyId));
    }

    @Test
    public void testExistsById_True() {
        Long id = 1L;
        when(userRepository.existsById(id)).thenReturn(true);

        Boolean result = userService.existsById(id);

        assertTrue(result);
    }

    @Test
    public void testExistsById_False() {
        Long id = 1L;
        when(userRepository.existsById(id)).thenReturn(false);

        Boolean result = userService.existsById(id);

        assertFalse(result);
    }

    @Test
    public void testGetAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();
        user.setId(1L);
        user.setCompanyId(1L);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        CompanyResponse companyResponse = new CompanyResponse(1L, "TestCompany", 100000L);
        UserResponse userResponse = new UserResponse(1L, "Ivan", "Ivanov", "+79021111111", companyResponse);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(companyClient.getCompanyById(1L)).thenReturn(companyResponse);
        when(userMapper.toResponse(user, companyResponse)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Ivan", result.getContent().getFirst().getFirstName());
    }

    @Test
    public void testGetAllUsers_CompanyIdNull() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();
        user.setId(1L);
        user.setCompanyId(null);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        UserResponse userResponse = new UserResponse(1L, "Ivan", "Ivanov", "+79021111111", null);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user, null)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().getFirst().getCompany());
    }

    @Test
    public void testHandleUserCreatedEvent_Success() {
        UserServiceImpl.UserCreatedEvent event = new UserServiceImpl.UserCreatedEvent(1L, 1L);
        doNothing().when(companyClient).addEmployeeToCompany(1L, 1L);

        userService.handleUserCreatedEvent(event);

        verify(companyClient).addEmployeeToCompany(1L, 1L);
    }
}