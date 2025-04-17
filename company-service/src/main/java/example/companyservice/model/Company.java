package example.companyservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "companies")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long budget;

    @ElementCollection
    @CollectionTable(name = "company_employees", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "user_id")
    private Set<Long> employeeIds = new HashSet<>();
}
