package example.companyservice.controller;

import example.companyservice.dto.CompanyRequest;
import example.companyservice.dto.CompanyResponse;
import example.companyservice.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyRequest request) {
        log.info("Creating company - name: {}", request.getName());
        CompanyResponse response = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable Long id) {
        log.info("Fetching company - id: {}", id);
        CompanyResponse response = companyService.getCompanyById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CompanyResponse>> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.info("Fetching all companies - page: {}, size: {}, sort: {}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(companyService.getAllCompanies(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequest request) {
        log.info("Updating company - id: {}, new name: {}", id, request.getName());
        CompanyResponse response = companyService.updateCompany(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        log.info("Deleting company - id: {}", id);
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<Void> addEmployeeToCompany(
            @PathVariable Long companyId,
            @PathVariable Long employeeId) {
        log.info("Adding employee to company - companyId: {}, employeeId: {}", companyId, employeeId);
        companyService.addEmployeeToCompany(companyId, employeeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<Void> removeEmployeeFromCompany(
            @PathVariable Long companyId,
            @PathVariable Long employeeId) {
        log.info("Removing employee from company - companyId: {}, employeeId: {}", companyId, employeeId);
        companyService.removeEmployeeFromCompany(companyId, employeeId);
        return ResponseEntity.noContent().build();
    }
}