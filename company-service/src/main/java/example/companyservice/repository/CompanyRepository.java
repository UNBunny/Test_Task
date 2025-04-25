package example.companyservice.repository;

import example.companyservice.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    Boolean existsByName(String name);

    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.employeeIds")
    Page<Company> findAllWithEmployees(Pageable pageable);
}