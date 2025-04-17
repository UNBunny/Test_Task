package example.userservice.client;

import example.userservice.dto.CompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "company-service", url = "http://localhost:8882")
public interface CompanyClient {
    @GetMapping("/api/companies/{id}")
    CompanyResponse getCompanyById(@PathVariable("id") Long id);

    @PostMapping("/api/companies/{companyId}/employees/{employeeId}")
    void addEmployeeToCompany(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId);

    @DeleteMapping("/api/companies/{companyId}/employees/{employeeId}")
    void removeEmployeeFromCompany(@PathVariable Long companyId, @PathVariable Long employeeId);


}