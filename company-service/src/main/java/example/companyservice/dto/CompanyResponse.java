package example.companyservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyResponse {
    private Long id;
    private String name;
    private Long budget;
    private List<UserResponse> employees;
}
