package example.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    Long id;
    String firstName;
    String lastName;
    String phoneNumber;
    CompanyResponse company;
}