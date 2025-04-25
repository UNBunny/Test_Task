package example.companyservice.client;

import example.companyservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "user-service", url = "http://user-service:8881")
public interface UserClient {
    @GetMapping("/api/users/{id}")
    UserResponse findByIdWithCompany(@PathVariable("id") Long id);

    @GetMapping("/api/users/batch")
    Map<Long, UserResponse> getUsersBatch(@RequestParam("ids") Set<Long> ids);

    @GetMapping("/api/users/exists/{id}")
    Boolean existsById(@PathVariable Long id);

    @PostMapping("/api/users/{companyId}/employees/{employeeId}")
    void addCompanyToUser(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId);
}