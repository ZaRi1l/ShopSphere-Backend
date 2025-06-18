package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
