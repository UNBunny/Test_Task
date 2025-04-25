package example.companyservice;

import example.companyservice.client.UserClient;
import example.companyservice.dto.CompanyRequest;
import example.companyservice.dto.CompanyResponse;
import example.companyservice.dto.UserResponse;
import example.companyservice.exception.CompanyNameExistsException;
import example.companyservice.exception.CompanyNotFoundException;
import example.companyservice.exception.EmployeeAlreadyExistsException;
import example.companyservice.exception.UserNotFoundException;
import example.companyservice.mapper.CompanyMapper;
import example.companyservice.model.Company;
import example.companyservice.repository.CompanyRepository;
import example.companyservice.service.CompanyServiceImpl;
import example.companyservice.service.CompanyServiceImpl.CompanyCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@Slf4j
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private final CompanyRequest testRequest = new CompanyRequest(
            "Test Company",
            100000L,
            new HashSet<>(Set.of(1L))
    );

    private final Company testCompany = Company.builder()
            .id(1L)
            .name("Test Company")
            .budget(100000L)
            .employeeIds(new HashSet<>(Set.of(1L)))
            .build();


    @Test
    void testCreateCompany_Success() {
        CompanyResponse expectedResponse = new CompanyResponse(1L, "Test Company", 100000L, List.of());

        when(companyRepository.existsByName(anyString())).thenReturn(false);
        when(userClient.existsById(anyLong())).thenReturn(true);
        when(companyMapper.toEntity(any())).thenReturn(testCompany);
        when(companyRepository.save(any())).thenReturn(testCompany);
        when(companyMapper.toResponse(eq(testCompany), anyList())).thenReturn(expectedResponse);

        CompanyResponse response = companyService.createCompany(testRequest);

        assertEquals("Test Company", response.getName());
        assertEquals(1L, response.getId());

        verify(companyRepository).save(testCompany);
        verify(eventPublisher).publishEvent(any(CompanyCreatedEvent.class));
        assertNotNull(response);
    }


    @Test
    void testCreateCompany_ThrowWhenNameExists() {
        when(companyRepository.existsByName(testRequest.getName())).thenReturn(true);

        assertThrows(CompanyNameExistsException.class,
                () -> companyService.createCompany(testRequest));
    }

    @Test
    void testCreateCompany_ThrowWhenUserNotExists() {
        when(companyRepository.existsByName(anyString())).thenReturn(false);
        when(userClient.existsById(anyLong())).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> companyService.createCompany(testRequest));
    }

    @Test
    void testGetCompanyById_ThrowWhenNotFound() {
        when(companyRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class,
                () -> companyService.getCompanyById(1L));
    }

    @Test
    void testGetCompanyById_ReturnEnrichedResponse() {

        UserResponse userResponse = new UserResponse(1L, "Ivan", "Ivanov", "+79021111111");
        CompanyResponse expectedResponse = new CompanyResponse(
                1L,
                "Test Company",
                10000L,
                List.of(userResponse)
        );
        when(companyRepository.findById(anyLong())).thenReturn(Optional.of(testCompany));
        when(userClient.getUsersBatch(anySet())).thenReturn(Map.of(
                1L, new UserResponse()
        ));
        when(companyMapper.toResponse(eq(testCompany), anyList())).thenReturn(expectedResponse);

        CompanyResponse response = companyService.getCompanyById(1L);

        assertNotNull(response);
        assertEquals(1, response.getEmployees().size());
    }

    @Test
    void testUpdateCompany_ThrowWhenNameConflict() {
        Company existing = testCompany.toBuilder().name("Old Name").build();
        CompanyRequest updateRequest = new CompanyRequest(
                "New Name",
                150000L,
                Set.of(1L)
        );

        when(companyRepository.findById(anyLong())).thenReturn(Optional.of(existing));
        when(companyRepository.existsByName("New Name")).thenReturn(true);

        assertThrows(CompanyNameExistsException.class,
                () -> companyService.updateCompany(1L, updateRequest));
    }

    @Test
    void testUpdateCompany_Success() {
        when(companyRepository.findById(anyLong())).thenReturn(Optional.of(testCompany));
        when(companyRepository.save(any())).thenReturn(testCompany);

        companyService.updateCompany(1L, testRequest);

        verify(companyMapper).updateEntity(eq(testRequest), eq(testCompany));
        verify(companyRepository).save(testCompany);
    }


    @Test
    void testAddEmployee_ThrowWhenAlreadyExists() {
        when(companyRepository.findById(anyLong())).thenReturn(Optional.of(testCompany));

        assertThrows(EmployeeAlreadyExistsException.class,
                () -> companyService.addEmployeeToCompany(1L, 1L));
    }

    @Test
    void testRemoveEmployee_IgnoreWhenNotExists() {
        when(companyRepository.findById(anyLong())).thenReturn(Optional.of(testCompany));

        companyService.removeEmployeeFromCompany(1L, 1L);

        verify(companyRepository).save(testCompany);
    }

    @Test
    void testHandleCompanyCreatedEvent_CallUserClient() {
        CompanyCreatedEvent event = new CompanyCreatedEvent(1L, Set.of(1L, 2L));

        companyService.handleCompanyCreatedEvent(event);

        verify(userClient, times(2)).addCompanyToUser(anyLong(), anyLong());
    }
}