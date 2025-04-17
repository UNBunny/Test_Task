package example.userservice.controller;

import example.userservice.dto.UserRequest;
import example.userservice.dto.UserResponse;
import example.userservice.repository.UserRepository;
import example.userservice.service.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserServiceImpl userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("Creating user with phone: {}, companyId: {}", userRequest.getPhoneNumber(), userRequest.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userRequest));
    }

    @PostMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<Void> addCompanyToUser(@PathVariable("companyId") Long companyId, @PathVariable("employeeId") Long employeeId) {
        log.info("Adding company {} to user {}", companyId, employeeId);
        userService.addCompanyToUser(employeeId, companyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        log.info("Fetching user by ID: {}", id);
        return ResponseEntity.ok(userService.findByIdWithCompany(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.info("Fetching users, page: {}, size: {}, sort: {}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest) {
        log.info("Updating user with id: {}, new data: {}", id, userRequest);
        return ResponseEntity.ok(userService.updateUser(id, userRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/batch")
    public ResponseEntity<Map<Long, UserResponse>> getUsersBatch(@RequestParam Set<Long> ids) {
        log.info("Batch fetch users: {}", ids);
        return ResponseEntity.ok(userService.getUsersByIds(ids));
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<Boolean> userExists(@PathVariable Long id) {
        log.info("Checking if user with id: {} exists", id);
        return ResponseEntity.ok(userService.existsById(id));
    }

}