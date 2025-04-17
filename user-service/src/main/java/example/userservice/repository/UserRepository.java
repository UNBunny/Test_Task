package example.userservice.repository;

import example.userservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    Boolean existsByPhoneNumber(String phoneNumber);

    Page<User> findAll(Pageable pageable);

}
