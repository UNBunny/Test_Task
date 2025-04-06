package example.companyservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "user-service", url = "http://company-service:8881")
public interface UserClient {
    @GetMapping("/api/users/{id}/raw")
    Map<String, Object> getUserRawData(@PathVariable("id") Long id);
}