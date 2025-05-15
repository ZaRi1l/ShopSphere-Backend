package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart_Id(Integer cartId);
}
