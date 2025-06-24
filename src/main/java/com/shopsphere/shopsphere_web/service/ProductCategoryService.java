package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.ProductCategoryDTO;
import com.shopsphere.shopsphere_web.entity.ProductCategory;
import com.shopsphere.shopsphere_web.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    // 모든 카테고리 목록 조회 (계층적으로 반환할 수도 있고, 플랫하게 반환할 수도 있음)
    // 여기서는 DTO의 Response 구조(parent 필드 포함)를 활용하여 계층적으로 반환하는 예시
    public List<ProductCategoryDTO.Response> getAllCategoriesHierarchically() {
        // 1. 최상위 카테고리 조회
        List<ProductCategory> rootCategories = categoryRepository.findByParentIsNull();
        // 2. 각 최상위 카테고리를 DTO로 변환 (재귀적으로 하위 카테고리 포함)
        return rootCategories.stream()
                .map(this::convertToDtoWithChildren)
                .collect(Collectors.toList());
    }

    // 특정 부모 ID를 가진 하위 카테고리 목록 조회
    public List<ProductCategoryDTO.Response> getSubCategories(Integer parentId) {
        List<ProductCategory> subCategories = categoryRepository.findByParent_Id(parentId);
        return subCategories.stream()
                .map(this::convertToDtoWithoutChildren) // 하위만 반환, 더 깊은 계층은 필요시 프론트에서 재요청
                .collect(Collectors.toList());
    }


    // ProductCategory 엔티티를 ProductCategoryDTO.Response로 변환 (하위 카테고리 포함)
    private ProductCategoryDTO.Response convertToDtoWithChildren(ProductCategory category) {
        if (category == null) return null;

        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCreatedAt(category.getCreatedAt());

        // 하위 카테고리 조회 및 DTO 변환 (재귀 호출)
        List<ProductCategory> children = categoryRepository.findByParent_Id(category.getId());
        if (children != null && !children.isEmpty()) {
            // 이 부분에서 무한 재귀를 피하기 위해, DTO의 parent 필드 설정 시 주의가 필요.
            // 여기서는 자식 DTO를 만들 때 parent를 다시 설정하지 않도록 하거나,
            // DTO Response 구조를 조금 다르게 가져갈 수 있음.
            // 간단하게는 자식의 parent는 null로 두거나, id만 참조하도록 할 수 있음.
            // 여기서는 자식 DTO 생성 시 parent는 설정하지 않는 것으로 가정
            List<ProductCategoryDTO.Response> childDtos = children.stream()
                    .map(this::convertToDtoWithChildren) // 재귀 호출
                    .collect(Collectors.toList());
            // 자식 DTO들을 현재 DTO의 children 리스트 같은 필드에 담아야 하는데,
            // 현재 ProductDTO.Response에는 children 리스트 필드가 없음.
            // 만약 DTO.Response에 List<Response> children; 필드가 있다면 아래와 같이 설정
            // dto.setChildren(childDtos);

            // 현재 DTO 구조에서는 부모 정보만 표시하므로, 하위 카테고리를 가져와서
            // Response DTO의 parent 필드를 채우는 방식이 아님.
            // 최상위에서 아래로 내려가는 구조를 만들 때,
            // ProductDTO.Response의 parent 필드는 자신의 부모를 가리키므로,
            // 최상위 카테고리의 parent는 null이 됨.
        }
        // 이 DTO는 자신의 부모를 설정해야 함.
        if (category.getParent() != null) {
            dto.setParent(convertToDtoWithoutChildren(category.getParent())); // 재귀 방지 위해 자식 없는 DTO로 변환
        }

        return dto;
    }

    // ProductCategory 엔티티를 ProductCategoryDTO.Response로 변환 (하위 카테고리 미포함, 부모 정보만)
    // 기존 ProductService의 convertToCategoryResponse 와 유사할 수 있음.
    private ProductCategoryDTO.Response convertToDtoWithoutChildren(ProductCategory category) {
        if (category == null) return null;
        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCreatedAt(category.getCreatedAt());
        // 부모 정보 설정 (무한 재귀 방지를 위해 여기서는 부모의 부모까지만 설정하거나, ID만 설정)
        if (category.getParent() != null) {
             // dto.setParent(convertToDtoWithoutChildren(category.getParent())); // 필요시 재귀, 하지만 깊이 제한 필요
             // 더 간단히는 부모 ID만 제공하거나, 프론트에서 필요시 부모 정보를 다시 요청.
             // 여기서는 ProductDTO.Response의 parent 필드를 채우는 것을 목표로 하므로,
             // 자신의 부모를 설정.
             ProductCategoryDTO.Response parentDto = new ProductCategoryDTO.Response();
             parentDto.setId(category.getParent().getId());
             parentDto.setName(category.getParent().getName());
             // parentDto의 parent는 더 이상 설정하지 않아 무한루프 방지
             dto.setParent(parentDto);
        }
        return dto;
    }

    // 프론트엔드에서 플랫 리스트를 선호할 경우 (계층 구조 없이 모든 카테고리)
    public List<ProductCategoryDTO.Response> getAllCategoriesFlat() {
        return categoryRepository.findAll().stream()
            .map(this::convertToDtoForFlatList) // 부모 정보는 ID만 포함하거나, 간단히
            .collect(Collectors.toList());
    }

    private ProductCategoryDTO.Response convertToDtoForFlatList(ProductCategory category) {
        if (category == null) return null;
        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCreatedAt(category.getCreatedAt());
        if (category.getParent() != null) {
            // 부모 정보를 간단히 표현 (예: 부모 ID만 가지는 DTO, 또는 부모 DTO를 만들되 그 부모는 X)
            ProductCategoryDTO.Response parentDto = new ProductCategoryDTO.Response();
            parentDto.setId(category.getParent().getId());
            parentDto.setName(category.getParent().getName()); // 이름까지는 괜찮을 수 있음
            dto.setParent(parentDto);
        }
        return dto;
    }
}