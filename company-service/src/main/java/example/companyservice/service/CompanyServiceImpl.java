package example.companyservice.service;

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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserClient userClient;
    private final CompanyMapper companyMapper;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAllCompanies(Pageable pageable) {
        Page<Company> companyPage = companyRepository.findAllWithEmployees(pageable);
        List<CompanyResponse> enrichedContent = enrichCompaniesWithEmployees(companyPage.getContent());

        log.info("Fetched {} companies for page {}", enrichedContent.size(), pageable.getPageNumber());
        return new PageImpl<>(enrichedContent, companyPage.getPageable(), companyPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        List<UserResponse> employees = fetchEmployeesForCompany(company.getEmployeeIds());
        CompanyResponse response = companyMapper.toResponse(company, employees);

        log.info("Fetched company with id: {}", id);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyResponse createCompany(CompanyRequest request) {
        log.debug("Creating new company: {}", request.getName());

        if (companyRepository.existsByName(request.getName())) {
            throw new CompanyNameExistsException(request.getName());
        }

        Company company = companyMapper.toEntity(request);

        if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
            for (Long employeeId : request.getEmployeeIds()) {
                if (!checkUserExists(employeeId)) {
                    throw new UserNotFoundException(employeeId);
                }
            }
            company.setEmployeeIds(request.getEmployeeIds());
        }

        company = companyRepository.save(company);

        // Публикуем событие вместо синхронного вызова
        if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
            eventPublisher.publishEvent(new CompanyCreatedEvent(company.getId(), request.getEmployeeIds()));
        }

        List<UserResponse> employees = fetchEmployeesForCompany(company.getEmployeeIds());
        CompanyResponse response = companyMapper.toResponse(company, employees);
        log.info("Created new company with id: {}", company.getId());
        return response;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCompanyCreatedEvent(CompanyCreatedEvent event) {
        if (event.employeeIds() == null || event.employeeIds().isEmpty()) {
            return;
        }

        for (Long employeeId : event.employeeIds()) {
            try {
                userClient.addCompanyToUser(event.companyId(), employeeId);
                log.info("Successfully added company {} to user {}", event.companyId(), employeeId);
            } catch (FeignException e) {
                log.error("Failed to add company {} to user {}", event.companyId(), employeeId, e);

            }
        }
    }
    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        if (!existingCompany.getName().equals(request.getName()) &&
                companyRepository.existsByName(request.getName())) {
            throw new CompanyNameExistsException(request.getName());
        }

        companyMapper.updateEntity(request, existingCompany);
        Company updatedCompany = companyRepository.save(existingCompany);

        log.info("Updated company with id: {}", id);
        return companyMapper.toResponse(updatedCompany, fetchEmployeesForCompany(updatedCompany.getEmployeeIds()));
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new CompanyNotFoundException(id);
        }

        companyRepository.deleteById(id);
        log.info("Deleted company with id: {}", id);
    }

    @Override
    @Transactional
    public void addEmployeeToCompany(Long companyId, Long employeeId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));

        if (company.getEmployeeIds().contains(employeeId)) {
            throw new EmployeeAlreadyExistsException(employeeId, companyId);
        }

        if (!checkUserExists(employeeId)) {
            throw new UserNotFoundException(employeeId);
        }

        company.getEmployeeIds().add(employeeId);
        companyRepository.save(company);
        log.info("Added employee {} to company {}", employeeId, companyId);
    }


    @Override
    @Transactional
    public void removeEmployeeFromCompany(Long companyId, Long employeeId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));

        if (!company.getEmployeeIds().remove(employeeId)) {
            log.warn("Employee {} not found in company {}", employeeId, companyId);
            return;
        }

        companyRepository.save(company);
        log.info("Removed employee {} from company {}", employeeId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CompanyResponse> getCompaniesByIds(Set<Long> ids) {
        Map<Long, CompanyResponse> result = companyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        Company::getId,
                        company -> companyMapper.toResponse(company, fetchEmployeesForCompany(company.getEmployeeIds()))
                ));

        log.info("Fetched {} companies by ids: {}", result.size(), ids);
        return result;
    }

    private List<CompanyResponse> enrichCompaniesWithEmployees(List<Company> companies) {
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> allEmployeeIds = companies.stream()
                .flatMap(c -> c.getEmployeeIds().stream())
                .collect(Collectors.toSet());
        log.debug("Collected {} unique employee IDs", allEmployeeIds.size());

        Map<Long, UserResponse> employeesMap = fetchEmployeesData(allEmployeeIds);

        List<CompanyResponse> enrichedCompanies = companies.stream()
                .map(company -> {
                    List<UserResponse> companyEmployees = company.getEmployeeIds().stream()
                            .map(employeesMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return new CompanyResponse(
                            company.getId(),
                            company.getName(),
                            company.getBudget(),
                            companyEmployees
                    );
                })
                .collect(Collectors.toList());

        log.info("Enriched {} companies with employee data", enrichedCompanies.size());
        return enrichedCompanies;
    }

    private Map<Long, UserResponse> fetchEmployeesData(Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            log.debug("No employee IDs provided, returning empty map");
            return Collections.emptyMap();
        }
        try {
            log.debug("Fetching employee data for IDs: {}", employeeIds);
            return userClient.getUsersBatch(employeeIds);
        } catch (FeignException e) {
            log.error("Failed to fetch employee data for IDs: {} - HTTP Status: {}", employeeIds, e.status(), e);
            return Collections.emptyMap();
        }
    }

    private List<UserResponse> fetchEmployeesForCompany(Set<Long> employeeIds) {
        Map<Long, UserResponse> employeesMap = fetchEmployeesData(employeeIds);
        return employeeIds.stream()
                .map(employeesMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private Boolean checkUserExists(Long employeeId) {
        log.debug("Checking existence of user with ID: {}", employeeId);
        try {
            Boolean exists = userClient.existsById(employeeId);
            if (exists == null) {
                log.warn("Received null from userClient.existsById for employeeId: {}", employeeId);
                return false;
            }
            log.debug("User existence check for ID {}: {}", employeeId, exists);
            return exists;
        } catch (FeignException e) {
            log.error("Feign error checking user existence for ID {}: {}", employeeId, e.status(), e);
            if (e.status() == 404) {
                return false;
            }
            throw e;
        }
    }
    public record CompanyCreatedEvent(Long companyId, Set<Long> employeeIds) {}
}