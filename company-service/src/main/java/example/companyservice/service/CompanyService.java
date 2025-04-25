package example.companyservice.service;

import example.companyservice.dto.CompanyRequest;
import example.companyservice.dto.CompanyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Set;

public interface CompanyService {
    CompanyResponse createCompany(CompanyRequest request);

    CompanyResponse getCompanyById(Long id);

    Page<CompanyResponse> getAllCompanies(Pageable pageable);

    CompanyResponse updateCompany(Long id, CompanyRequest request);

    void deleteCompany(Long id);

    void addEmployeeToCompany(Long id, Long employeeId);

    void removeEmployeeFromCompany(Long id, Long employeeId);

    Map<Long, CompanyResponse> getCompaniesByIds(Set<Long> ids);
}
