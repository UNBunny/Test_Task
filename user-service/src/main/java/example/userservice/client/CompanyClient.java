package example.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "company-service", url = "http://company-service:8882")
public interface CompanyClient {
    @GetMapping("/api/companies/{id}/raw")
    Map<String, Object> getCompanyRawData(@PathVariable("id") Long id);
}