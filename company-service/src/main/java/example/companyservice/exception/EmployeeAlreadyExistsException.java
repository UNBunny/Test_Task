package example.companyservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmployeeAlreadyExistsException extends RuntimeException {
    public EmployeeAlreadyExistsException(Long employeeId, Long companyId) {
        super("Employee with id " + employeeId + " already exists in company " + companyId);
    }
}
