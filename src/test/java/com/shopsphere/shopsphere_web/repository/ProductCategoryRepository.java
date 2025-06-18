package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
}
