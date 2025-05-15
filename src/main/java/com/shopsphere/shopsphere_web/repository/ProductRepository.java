package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory_Id(Integer categoryId);
    List<Product> findByUser_Id(String userId);
}
