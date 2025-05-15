package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_Id(Integer orderId);
}
