package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
    List<ProductCategory> findByParent_Id(Integer parentId);
}
