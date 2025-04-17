package example.companyservice.mapper;

import example.companyservice.dto.CompanyRequest;
import example.companyservice.dto.CompanyResponse;
import example.companyservice.dto.UserResponse;
import example.companyservice.model.Company;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class CompanyMapper {

    public Company toEntity(CompanyRequest request) {
        if (request == null) {
            return null;
        }

        return Company.builder()
                .name(request.getName())
                .budget(request.getBudget())
                .employeeIds(extractEmployeeIds(request))
                .build();
    }

    public void updateEntity(CompanyRequest request, Company company) {
        if (request == null || company == null) {
            return;
        }

        company.setName(request.getName());
        company.setBudget(request.getBudget());
        company.setEmployeeIds(extractEmployeeIds(request));
    }

    public CompanyResponse toResponse(Company company) {
        return toResponse(company, null);
    }

    public CompanyResponse toResponse(Company company, List<UserResponse> employees) {
        if (company == null) {
            return null;
        }

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .budget(company.getBudget())
                .employees(Optional.ofNullable(employees).orElse(Collections.emptyList()))
                .build();
    }

    public CompanyRequest toRequest(Company company) {
        if (company == null) {
            return null;
        }

        return new CompanyRequest(
                company.getName(),
                company.getBudget(),
                new HashSet<>(company.getEmployeeIds())
        );
    }

    private Set<Long> extractEmployeeIds(CompanyRequest request) {
        return new HashSet<>(Optional.ofNullable(request.getEmployeeIds())
                .orElse(Collections.emptySet()));
    }
}