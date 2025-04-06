package example.companyservice.dto;

import example.companyservice.model.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.util.HashSet;
import java.util.Set;

@Builder
public record CompanyRequest(
        @NotBlank(message = "Name is required") String name,
        @Positive Long budget,
        Set<Long> employeeIds
) {
    public Company toEntity() {
        return new Company(null, name, budget, employeeIds != null ? employeeIds : new HashSet<>());
    }
}

