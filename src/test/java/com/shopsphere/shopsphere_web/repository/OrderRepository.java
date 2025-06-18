package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
