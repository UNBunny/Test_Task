package example.companyservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CompanyNameExistsException extends RuntimeException {
    public CompanyNameExistsException(String name) {
        super("Company name already exists: " + name);
    }
}