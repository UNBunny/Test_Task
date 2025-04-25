package example.companyservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Company name must be less than 50 characters")
    private String name;

    @Positive
    private Long budget;

    private Set<Long> employeeIds;
}

