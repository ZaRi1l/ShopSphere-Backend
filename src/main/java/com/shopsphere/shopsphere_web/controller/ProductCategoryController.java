package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductCategoryDTO;
import com.shopsphere.shopsphere_web.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    // 모든 카테고리 목록 조회 (플랫 리스트)
    @GetMapping
    public ResponseEntity<List<ProductCategoryDTO.Response>> getAllCategories() {
        List<ProductCategoryDTO.Response> categories = categoryService.getAllCategoriesFlat();
        return ResponseEntity.ok(categories);
    }

    // 특정 카테고리의 하위 카테고리 목록 조회
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<List<ProductCategoryDTO.Response>> getSubCategories(@PathVariable Integer parentId) {
        List<ProductCategoryDTO.Response> categories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(categories);
    }

    // (필요하다면) 계층적 카테고리 조회 API
    // @GetMapping("/hierarchical")
    // public ResponseEntity<List<ProductCategoryDTO.Response>> getAllCategoriesHierarchically() {
    //     List<ProductCategoryDTO.Response> categories = categoryService.getAllCategoriesHierarchically();
    //     return ResponseEntity.ok(categories);
    // }
}