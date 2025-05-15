package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}