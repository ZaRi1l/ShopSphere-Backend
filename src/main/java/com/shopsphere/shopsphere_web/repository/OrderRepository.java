package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUser_Id(String userId);
    // findById는 JpaRepository에서 이미 제공하므로 제거
}
