package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
