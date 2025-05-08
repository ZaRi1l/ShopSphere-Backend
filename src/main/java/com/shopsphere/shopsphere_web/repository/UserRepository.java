package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
}