// src/main/java/com/shopsphere/shopsphere_web/service/ProductService.java
package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.*; // UserDTO 등을 위해 와일드카드 사용 또는 개별 import
import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort; // Sort 임포트

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨 기본 트랜잭션: 읽기 전용
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository; // ProductImageRepository 주입
    private final ProductOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 새로운 상품을 생성합니다. (이미지 정보 포함)
     */
    @Transactional // 쓰기 트랜잭션 적용
    public ProductDTO.Response createProduct(String userId, ProductDTO.CreateRequest request) {
        // 1. 판매자(User) 조회
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // 2. 카테고리(ProductCategory) 조회
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));

        // 3. Product 엔티티 생성 및 기본 정보 설정
        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                // .imageUrl(request.getImageUrl()) // Product 엔티티에서 대표 이미지 필드를 제거하고 ProductImage로 통합 관리한다면 이 줄은 불필요
                .user(seller)
                .salesVolume(0) // 초기 판매량은 0
                .createdAt(LocalDateTime.now())
                .images(new ArrayList<>()) // 이미지 리스트 초기화
                .build();

        // 4. Product 엔티티 저장 (이미지 저장 전 Product ID 확보)
        Product savedProduct = productRepository.save(product);

        // 5. ProductImage 엔티티 생성 및 저장
        // ProductDTO.CreateRequest에 이미지 URL 관련 필드가 있다고 가정
        // 예: request.getImageUrl() (대표 이미지), request.getAdditionalImageUrls() (List<String> 추가 이미지)
        List<ProductImage> productImages = new ArrayList<>();
        int displayOrder = 0;

        // 대표 이미지 처리 (request.getImageUrl()이 있고, Product 엔티티의 imageUrl 필드를 사용하지 않는 경우)
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            ProductImage mainImage = ProductImage.builder()
                    .product(savedProduct)
                    .imageUrl(request.getImageUrl())
                    .displayOrder(displayOrder++)
                    // .isRepresentative(true) // 대표 이미지 플래그가 있다면 설정
                    .build();
            productImages.add(productImageRepository.save(mainImage));
        }

        // 추가 이미지 처리 (request.getAdditionalImageUrls()가 있다고 가정)
        // 실제로는 ProductDTO.CreateRequest.getImages() 와 같이 List<ProductImageDTO.CreateRequest> 를 받는 것이 더 유연함
        // 아래 코드는 List<String> additionalImageUrls 가 있다고 가정
        /*
        if (request.getAdditionalImageUrls() != null) {
            for (String subImageUrl : request.getAdditionalImageUrls()) {
                if (subImageUrl != null && !subImageUrl.isEmpty()) {
                    ProductImage subImage = ProductImage.builder()
                            .product(savedProduct)
                            .imageUrl(subImageUrl)
                            .displayOrder(displayOrder++)
                            // .isRepresentative(false)
                            .build();
                    productImages.add(productImageRepository.save(subImage));
                }
            }
        }
        */
        // savedProduct.setImages(productImages); // JPA 관계 설정에 따라 자동으로 업데이트되거나, 명시적으로 설정
        // productRepository.save(savedProduct); // images 필드 업데이트를 위해 다시 저장 (선택적, Cascade 설정에 따라 다름)

        // 6. 상품 옵션(ProductOption) 저장 (기존 로직)
        if (request.getOptions() != null) {
            for (ProductOptionDTO.CreateRequest optionRequest : request.getOptions()) {
                ProductOption option = ProductOption.builder()
                        .product(savedProduct) // 저장된 product 사용
                        .size(optionRequest.getSize())
                        .stockQuantity(optionRequest.getStockQuantity())
                        .additionalPrice(optionRequest.getAdditionalPrice())
                        .build();
                optionRepository.save(option);
            }
        }

        // 7. 최종적으로 저장된 상품 정보(이미지 포함)를 반환하기 위해 다시 조회하거나,
        //    savedProduct 객체에 productImages를 설정한 후 변환
        //    가장 확실한 방법은 ID로 다시 조회하는 것 (Lazy Loading된 연관 엔티티를 Eager하게 가져오기 위해)
        Product finalProductWithImages = productRepository.findById(savedProduct.getId())
                .orElseThrow(() -> new IllegalStateException("Product not found after save, id: " + savedProduct.getId()));

        return convertToResponse(finalProductWithImages);
    }

    /**
     * Product 엔티티를 ProductDTO.Response 로 변환합니다. (이미지, 옵션, 리뷰 정보 포함)
     */
    private ProductDTO.Response convertToResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductDTO.Response response = new ProductDTO.Response();
        response.setId(product.getId());
        response.setCategory(convertToCategoryResponse(product.getCategory()));
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        // response.setImageUrl(product.getImageUrl()); // Product 엔티티에서 대표 이미지 필드를 제거했다면 이 줄은 제거
        response.setCreatedAt(product.getCreatedAt());
        response.setSeller(convertToUserResponse(product.getUser()));
        response.setSalesVolume(product.getSalesVolume());

        // 상품 이미지 목록 변환 (ProductImage -> ProductImageDTO)
        if (product.getImages() != null) {
            List<ProductImageDTO> imageDTOs = product.getImages().stream()
                    .map(this::convertToProductImageDTO)
                    .collect(Collectors.toList());
            response.setImages(imageDTOs);
        } else {
            response.setImages(new ArrayList<>()); // 이미지가 없을 경우 빈 리스트 설정
        }

        // 상품 옵션 목록 변환 (ProductOption -> ProductOptionDTO.Response)
        // Product 엔티티에 옵션 리스트가 직접 매핑되어 있지 않다면, 옵션은 별도로 조회해야 합니다.
        // Product 엔티티에 @OneToMany로 options 필드가 있다면 product.getOptions() 사용 가능
        // 여기서는 기존 방식대로 optionRepository에서 조회
        List<ProductOptionDTO.Response> optionResponses = optionRepository.findByProduct_Id(product.getId())
                .stream()
                .map(this::convertToOptionResponse)
                .collect(Collectors.toList());
        response.setOptions(optionResponses);

        // 리뷰 개수 및 평균 평점 설정
        response.setReviewCount(reviewRepository.countByProductId(product.getId()));
        response.setAverageRating(reviewRepository.findAverageRatingByProductId(product.getId()).orElse(0.0));

        // 관심 수 설정 (실제 구현 필요)
        response.setInterestCount(999L); // 예시: 임의의 값

        return response;
    }

    /**
     * ProductImage 엔티티를 ProductImageDTO 로 변환합니다.
     */
    private ProductImageDTO convertToProductImageDTO(ProductImage image) {
        if (image == null) return null;
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setDisplayOrder(image.getDisplayOrder());
        dto.setCreatedAt(image.getCreatedAt());
        // dto.setIsRepresentative(image.getIsRepresentative()); // isRepresentative 필드가 있다면 매핑
        return dto;
    }

    /**
     * ProductOption 엔티티를 ProductOptionDTO.Response 로 변환합니다.
     */
    private ProductOptionDTO.Response convertToOptionResponse(ProductOption option) {
        if (option == null) return null;
        ProductOptionDTO.Response dto = new ProductOptionDTO.Response();
        dto.setId(option.getId());
        // dto.setOptionName(option.getOptionName()); // ProductOption 엔티티에 optionName 필드가 있다면
        dto.setSize(option.getSize()); // size가 옵션의 이름 역할을 할 수도 있음
        dto.setStockQuantity(option.getStockQuantity());
        dto.setAdditionalPrice(option.getAdditionalPrice());
        return dto;
    }

    /**
     * ProductCategory 엔티티를 ProductCategoryDTO.Response 로 변환합니다.
     */
    private ProductCategoryDTO.Response convertToCategoryResponse(ProductCategory category) {
        if (category == null) return null;
        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCreatedAt(category.getCreatedAt());
        // 부모 카테고리 정보 (재귀 호출) - 필요하다면
        // if (category.getParent() != null) {
        //     dto.setParent(convertToCategoryResponse(category.getParent()));
        // }
        return dto;
    }

    /**
     * User 엔티티를 UserDTO.Response 로 변환합니다.
     */
    private com.shopsphere.shopsphere_web.dto.UserDTO.Response convertToUserResponse(User user) {
        if (user == null) return null;
        com.shopsphere.shopsphere_web.dto.UserDTO.Response dto = new com.shopsphere.shopsphere_web.dto.UserDTO.Response();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole().toString()); // Enum이라면 .name() 또는 .toString()
        return dto;
    }

    /**
     * 특정 ID의 상품 정보를 조회합니다.
     */
    public ProductDTO.Response getProduct(Integer productId) {
        return productRepository.findById(productId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    /**
     * 특정 카테고리에 속한 모든 상품 목록을 조회합니다.
     */
    public List<ProductDTO.Response> getProductsByCategory(Integer categoryId) {
        // Product 엔티티에 category 필드가 LAZY 로딩일 경우 N+1 문제 발생 가능성 있음
        // @EntityGraph 등으로 해결하거나, Repository에서 DTO로 직접 변환하는 쿼리 작성 고려
        return productRepository.findByCategory_Id(categoryId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 판매자가 등록한 모든 상품 목록을 조회합니다.
     */
    public List<ProductDTO.Response> getProductsBySeller(String userId) {
        return productRepository.findByUser_Id(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public List<ProductDTO.Response> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDTO.Response updateProduct(String userId, Integer productId, ProductDTO.UpdateRequest request) {
        return productRepository.findById(productId)
                .map(product -> {
                    // 상품 소유자 확인
                    if (!product.getUser().getId().equals(userId)) {
                        throw new SecurityException("상품을 수정할 권한이 없습니다.");
                    }

                    // 카테고리 업데이트
                    if (request.getCategoryId() != null) {
                        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
                        product.setCategory(category);
                    }
                    
                    product.setName(request.getName());
                    product.setDescription(request.getDescription());
                    product.setPrice(request.getPrice());
                    product.setStockQuantity(request.getStockQuantity());
                    product.setImageUrl(request.getImageUrl());

                    // Update options
                    if (request.getOptions() != null) {
                        for (ProductOptionDTO.UpdateRequest optionRequest : request.getOptions()) {
                            if (optionRequest.getId() != null) {
                                optionRepository.findById(optionRequest.getId())
                                        .ifPresent(option -> {
                                            option.setSize(optionRequest.getSize());
                                            option.setStockQuantity(optionRequest.getStockQuantity());
                                            option.setAdditionalPrice(optionRequest.getAdditionalPrice());
                                        });
                            }
                        }
                    }

                    return convertToResponse(product);
                })
                .orElse(null);
    }

    @Transactional
    public void deleteProduct(String userId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
                

                System.out.println("삭제 시도1: " + productId);
        // 상품 소유자 확인
        if (!product.getUser().getId().equals(userId)) {
            throw new SecurityException("상품을 삭제할 권한이 없습니다.");
        }
        
        System.out.println("삭제 시도2: " + productId);
        productRepository.deleteById(productId);
        System.out.println("삭제 완료: " + productId);
    }
    /**
     * 기존 상품 정보를 수정합니다.
     * (이미지 수정 로직은 현재 복잡성을 피해 단순화. 실제로는 더 정교한 처리 필요)
     */
    @Transactional // 쓰기 트랜잭션 적용
    public ProductDTO.Response updateProduct(Integer productId, ProductDTO.UpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        // 기본 정보 업데이트
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        // if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl()); // 대표 이미지 필드 관련

        // 카테고리 업데이트 (요청에 categoryId가 있다면)
        // if (request.getCategoryId() != null) {
        //     ProductCategory category = categoryRepository.findById(request.getCategoryId())
        //             .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));
        //     product.setCategory(category);
        // }

        // 이미지 업데이트 로직 (매우 중요하고 복잡한 부분)
        // 1. 기존 이미지 삭제 요청 처리
        // 2. 새로운 이미지 추가 처리
        // 3. 이미지 순서 변경 처리
        // ProductDTO.UpdateRequest에 이미지 관련 정보(삭제할 이미지 ID 목록, 추가할 이미지 URL/파일, 순서 정보 등)가 필요
        // 예시: 삭제 로직
        // if (request.getDeletedImageIds() != null) {
        //    request.getDeletedImageIds().forEach(imageId -> productImageRepository.deleteById(imageId));
        // }
        // 예시: 추가 로직 (createProduct와 유사하게)
        // if (request.getNewImageUrls() != null) { /* ... */ }

        // 옵션 업데이트 로직 (기존과 유사)
        if (request.getOptions() != null) {
            // 기존 옵션 삭제 또는 업데이트, 새로운 옵션 추가 로직 필요
            // 여기서는 단순화하여 기존 옵션의 필드만 업데이트하는 예시
            for (ProductOptionDTO.UpdateRequest optionRequest : request.getOptions()) {
                if (optionRequest.getId() != null) { // 기존 옵션 ID가 있는 경우
                    optionRepository.findById(optionRequest.getId())
                            .ifPresent(option -> {
                                if (optionRequest.getSize() != null) option.setSize(optionRequest.getSize());
                                if (optionRequest.getStockQuantity() != null) option.setStockQuantity(optionRequest.getStockQuantity());
                                if (optionRequest.getAdditionalPrice() != null) option.setAdditionalPrice(optionRequest.getAdditionalPrice());
                                optionRepository.save(option); // 변경 사항 저장
                            });
                } else { // 새로운 옵션 추가 (ID가 없는 경우)
                    // ProductOption newOption = ProductOption.builder()
                    //        .product(product)
                    //        .size(optionRequest.getSize())
                    //        .stockQuantity(optionRequest.getStockQuantity())
                    //        .additionalPrice(optionRequest.getAdditionalPrice())
                    //        .build();
                    // optionRepository.save(newOption);
                }
            }
        }
        // Product 엔티티 저장 (JPA 변경 감지에 의해 자동으로 업데이트될 수 있음)
        // productRepository.save(product); // 명시적으로 호출해도 무방

        return convertToResponse(productRepository.findById(product.getId())
                                     .orElseThrow(() -> new IllegalStateException("Product not found after update, id: " + product.getId())));
    }

    /**
     * 특정 상품을 삭제합니다.
     * (연관된 이미지, 옵션 등은 Product 엔티티의 Cascade 설정에 따라 함께 삭제될 수 있음)
     */
    @Transactional // 쓰기 트랜잭션 적용
    public void deleteProduct(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }
        // 연관된 ProductOption 삭제 (만약 Cascade 설정이 없다면 수동으로)
        // List<ProductOption> options = optionRepository.findByProduct_Id(productId);
        // optionRepository.deleteAll(options);

        // 연관된 ProductImage 삭제 (만약 Cascade 설정이 없다면 수동으로)
        // List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        // productImageRepository.deleteAll(images);

        productRepository.deleteById(productId);
    }

    /**
     * 키워드를 사용하여 상품명 또는 카테고리명에서 상품을 검색합니다.
     *
     * @param keyword 검색 키워드
     * @return 검색된 ProductDTO.Response 리스트
     */
    public List<ProductDTO.Response> searchProductsByKeyword(String keyword, String sortOption) { // sortOption 파라미터 추가
        List<Product> productsByName = productRepository.findByNameContainingIgnoreCase(keyword);
        List<ProductCategory> foundCategories = categoryRepository.findByNameContainingIgnoreCase(keyword);
        List<Product> productsByCategoryName = new ArrayList<>();
        if (!foundCategories.isEmpty()) {
            List<Integer> categoryIds = foundCategories.stream().map(ProductCategory::getId).collect(Collectors.toList());
            productsByCategoryName = productRepository.findByCategory_IdIn(categoryIds);
        }
    
        Set<Product> combinedProductSet = new HashSet<>(productsByName);
        combinedProductSet.addAll(productsByCategoryName);
    
        // 정렬 로직
        List<Product> sortedProducts = new ArrayList<>(combinedProductSet);
        switch (sortOption) {
            case "sales_volume_desc": // 판매량 많은 순 (인기순으로 대체 가능)
                sortedProducts.sort(Comparator.comparing(Product::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Product::getId));
                break;
            case "created_at_desc": // 최신 등록순
                sortedProducts.sort(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Product::getId));
                break;
            case "price_asc": // 낮은 가격순
                sortedProducts.sort(Comparator.comparing(Product::getPrice).thenComparing(Product::getId));
                break;
            case "price_desc": // 높은 가격순
                sortedProducts.sort(Comparator.comparing(Product::getPrice, Comparator.reverseOrder()).thenComparing(Product::getId));
                break;
            case "musinsa_recommend": // 무신사 추천순 (별도 로직 필요, 여기서는 기본 정렬 또는 판매량으로 대체)
            default: // 기본 정렬 (예: ID 또는 이름순, 또는 판매량 많은 순)
                sortedProducts.sort(Comparator.comparing(Product::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Product::getId)); // 기본은 판매량순
                break;
        }
    
        return sortedProducts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProductDTO.Response convertToProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        ProductDTO.Response dto = new ProductDTO.Response();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setSalesVolume(product.getSalesVolume());
    
        if (product.getCategory() != null) {
            dto.setCategory(convertToCategoryResponse(product.getCategory())); // ProductCategory -> ProductCategoryDTO.Response
        }
        if (product.getUser() != null) { // 상품 판매자 정보
            dto.setSeller(convertToUserResponse(product.getUser()));       // User -> UserDTO.Response
        }
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            dto.setImages(product.getImages().stream()
                // .map(this::convertToProductImageDTO) // ProductImage -> ProductImageDTO
                .map(imageEntity -> { // 간단히 ProductImageDTO를 직접 생성
                    ProductImageDTO imageDto = new ProductImageDTO();
                    imageDto.setId(imageEntity.getId());
                    imageDto.setImageUrl(imageEntity.getImageUrl());
                    imageDto.setDisplayOrder(imageEntity.getDisplayOrder());
                    return imageDto;
                })
                .collect(Collectors.toList()));
        }
        // ProductDTO.Response에 평균 평점, 리뷰 수 등이 있다면 ProductService에서 채워야 함
        // dto.setAverageRating(reviewRepository.findAverageRatingByProductId(product.getId()).orElse(0.0));
        // dto.setReviewCount(reviewRepository.countByProductId(product.getId()));
        // dto.setInterestCount(...)
        return dto;
    }
    
}