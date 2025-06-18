package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Integer> {
}
