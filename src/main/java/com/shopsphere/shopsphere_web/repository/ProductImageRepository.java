package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Integer productId);
}