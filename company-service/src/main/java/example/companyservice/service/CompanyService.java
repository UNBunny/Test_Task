package example.companyservice.service;


import example.companyservice.client.UserClient;
import example.companyservice.dto.CompanyRequest;
import example.companyservice.dto.CompanyResponse;
import example.companyservice.model.Company;
import example.companyservice.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserClient userClient;

    public List<CompanyResponse> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        return enrichCompaniesWithEmployees(companies);
    }

    public CompanyResponse findById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + id));

        List<Map<String, Object>> employees = new ArrayList<>();

        if (!company.getEmployeeIds().isEmpty()) {
            for (Long employeeId : company.getEmployeeIds()) {
                try {
                    Map<String, Object> userData = userClient.getUserRawData(employeeId);
                    employees.add(userData);
                } catch (Exception e) {
                    log.error("Failed to fetch data for employee: {}", employeeId, e);
                    employees.add(Map.of(
                            "id", employeeId,
                            "error", "Failed to load user data"
                    ));
                }
            }
        }

        return CompanyResponse.fromEntity(company, employees);
    }

    public CompanyResponse createCompany(CompanyRequest request) {
        if (companyRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Company name already exists");
        }

        Company company = companyRepository.save(request.toEntity());
        return CompanyResponse.fromEntity(company, null);
    }

    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        if (!existingCompany.getName().equals(request.name()) &&
                companyRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Company name already exists");
        }

        existingCompany.setName(request.name());
        existingCompany.setBudget(request.budget());
        existingCompany.setEmployeeIds(request.employeeIds());

        Company updatedCompany = companyRepository.save(existingCompany);
        return CompanyResponse.fromEntity(updatedCompany, null);
    }

    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new EntityNotFoundException("Company not found");
        }
        companyRepository.deleteById(id);
    }

    public Map<String, Object> getCompanyRawData(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        return Map.of(
                "id", company.getId(),
                "name", company.getName(),
                "budget", company.getBudget()
        );
    }


    private List<CompanyResponse> enrichCompaniesWithEmployees(List<Company> companies) {

        Set<Long> allEmployeeIds = companies.stream()
                .flatMap(company -> company.getEmployeeIds().stream())
                .collect(Collectors.toSet());


        Map<Long, Map<String, Object>> employeesData = fetchEmployeesData(allEmployeeIds);

        return companies.stream()
                .map(company -> {
                    if (company.getEmployeeIds().isEmpty()) {
                        return CompanyResponse.fromEntity(company, Collections.emptyList());
                    }

                    List<Map<String, Object>> companyEmployees = company.getEmployeeIds().stream()
                            .map(employeeId -> employeesData.getOrDefault(employeeId,
                                    Map.of("error", "Employee data not available")))
                            .toList();

                    return CompanyResponse.fromEntity(company, companyEmployees);
                })
                .toList();
    }

    private Map<Long, Map<String, Object>> fetchEmployeesData(Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Map<String, Object>> employees = new HashMap<>();

        for (Long employeeId : employeeIds) {
            try {
                Map<String, Object> employee = userClient.getUserRawData(employeeId);
                if (employee != null && employee.get("id") != null) {
                    employees.put(employeeId, employee);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch employees data", e);
                return Collections.emptyMap();
            }
        }
        return employees;
    }
}