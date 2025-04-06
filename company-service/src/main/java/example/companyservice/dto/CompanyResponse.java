package example.companyservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import example.companyservice.model.Company;
import lombok.*;

import java.util.List;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyResponse(
        Long id,
        String name,
        Long budget,
        List<Map<String, Object>> employees,
        String employeesLoadError
) {
    public static CompanyResponse fromEntity(Company company, List<Map<String, Object>> employees) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getBudget(),
                employees,
                null
        );
    }

}