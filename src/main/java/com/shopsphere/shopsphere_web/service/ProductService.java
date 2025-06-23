package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.*;
import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import com.shopsphere.shopsphere_web.specification.ProductSpecifications; // Specification 클래스
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification; // Specification 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository; // 리뷰 정보에 필요

    @Transactional
    public ProductDTO.Response createProduct(String userId, ProductDTO.CreateRequest request) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다: " + userId));
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.getCategoryId()));

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .user(seller)
                .salesVolume(0)
                .createdAt(LocalDateTime.now())
                .images(new ArrayList<>()) // 이미지 리스트 초기화
                .options(new ArrayList<>()) // 옵션 리스트 초기화
                .build();
        
        // 대표 이미지 URL이 있다면 Product 엔티티의 imageUrl 필드에 직접 저장 (이전 방식 유지 시)
        // 또는 ProductImage로 통합 관리한다면 아래 로직으로 변경
        // product.setImageUrl(request.getImageUrl()); 

        Product savedProduct = productRepository.save(product);

        // ProductImage 처리 (대표 이미지 + 추가 이미지)
        List<ProductImage> productImages = new ArrayList<>();
        int displayOrder = 0;
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            ProductImage mainImage = ProductImage.builder()
                    .product(savedProduct)
                    .imageUrl(request.getImageUrl())
                    .displayOrder(displayOrder++)
                    .createdAt(LocalDateTime.now())
                    .build();
            productImages.add(productImageRepository.save(mainImage));
        }
        if (request.getAdditionalImageUrls() != null) {
            for (String addImageUrl : request.getAdditionalImageUrls()) {
                if (addImageUrl != null && !addImageUrl.isEmpty()) {
                    ProductImage additionalImage = ProductImage.builder()
                            .product(savedProduct)
                            .imageUrl(addImageUrl)
                            .displayOrder(displayOrder++)
                            .createdAt(LocalDateTime.now())
                            .build();
                    productImages.add(productImageRepository.save(additionalImage));
                }
            }
        }
        // savedProduct.setImages(productImages); // Cascade 설정으로 자동 반영될 수도, 명시적 설정 후 재저장 필요할 수도

        // 옵션 처리
        if (request.getOptions() != null) {
            for (ProductOptionDTO.CreateRequest optionDto : request.getOptions()) {
                ProductOption option = ProductOption.builder()
                        .product(savedProduct)
                        .size(optionDto.getSize()) // 또는 optionName
                        .stockQuantity(optionDto.getStockQuantity())
                        .additionalPrice(optionDto.getAdditionalPrice())
                        .build();
                optionRepository.save(option);
            }
        }
        
        // ID로 다시 조회하여 모든 연관관계가 로드된 엔티티를 DTO로 변환
        return productRepository.findById(savedProduct.getId())
                .map(this::convertToResponseWithFetchedData) // N+1 주의하며 변환
                .orElseThrow(() -> new IllegalStateException("상품 저장 후 조회 실패"));
    }

    public Page<ProductDTO.Response> findProductsWithFiltersAndSort(
            Integer categoryId, Integer minPrice, Integer maxPrice, String sortOption, Pageable pageable) {

        Sort sort = Sort.unsorted();
        switch (sortOption.toLowerCase()) {
            case "sales_volume_desc":
                sort = Sort.by(Sort.Direction.DESC, "salesVolume").and(Sort.by(Sort.Direction.ASC, "id"));
                break;
            case "created_at_desc":
                sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
                break;
            case "price_asc":
                sort = Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.ASC, "id"));
                break;
            case "price_desc":
                sort = Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.ASC, "id"));
                break;
            case "musinsa_recommend":
            default:
                sort = Sort.by(Sort.Direction.DESC, "salesVolume").and(Sort.by(Sort.Direction.ASC, "id")); // 기본: 판매량순
                break;
        }
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<Product> spec = Specification.where(null); // 항상 true인 Specification으로 시작
        if (categoryId != null) {
            spec = spec.and(ProductSpecifications.withCategoryId(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.minPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.maxPrice(maxPrice));
        }
        
        // ProductRepository의 findAll(Specification, Pageable) 사용
        // 이 findAll은 N+1을 해결하기 위해 EntityGraph나 Fetch Join을 내부적으로 사용하도록 설정하거나,
        // DTO 프로젝션을 사용하는 커스텀 Repository 메소드로 대체해야 함.
        // 여기서는 기본 findAll을 사용하고, convertToResponseWithFetchedData에서 LAZY 로딩을 주의하도록 함.
        // 가장 좋은 방법은 Repository에서 DTO로 직접 프로젝션하는 것입니다.
        Page<Product> productPage = productRepository.findAll(spec, newPageable);

        List<ProductDTO.Response> dtoList = productPage.getContent().stream()
                .map(this::convertToResponseWithFetchedData) // N+1 주의하며 변환
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, newPageable, productPage.getTotalElements());
    }
    
    // N+1 문제가 해결된 convertToResponse (연관 엔티티가 EAGER 로딩되었거나, Fetch Join으로 가져왔거나, DTO 프로젝션인 경우)
    public ProductDTO.Response convertToResponseWithFetchedData(Product product) {
        if (product == null) return null;

        ProductDTO.Response dto = new ProductDTO.Response();
        dto.setId(product.getId());
        dto.setName(product.getName());
        // 목록에서는 description 제외 가능
        // dto.setDescription(product.getDescription()); 
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setSalesVolume(product.getSalesVolume());

        if (product.getCategory() != null) { // FetchType.LAZY라면 N+1 유발 가능성
            dto.setCategory(convertToCategoryResponse(product.getCategory()));
        }
        if (product.getUser() != null) { // FetchType.LAZY라면 N+1 유발 가능성
            dto.setSeller(convertToUserResponse(product.getUser()));
        }
        
        // 이미지: Product 엔티티의 images 필드가 LAZY 로딩이고, 여기서 접근하면 N+1 발생
        // EntityGraph나 Fetch Join으로 함께 가져와야 함.
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            dto.setImages(product.getImages().stream()
                .map(this::convertToProductImageDTO)
                .collect(Collectors.toList()));
        } else {
             dto.setImages(new ArrayList<>());
        }

        // 옵션: 목록에서 필요 없다면 제외
        // if (product.getOptions() != null && !product.getOptions().isEmpty()) { // LAZY 로딩 시 N+1
        //     dto.setOptions(product.getOptions().stream()
        //         .map(this::convertToOptionResponse)
        //         .collect(Collectors.toList()));
        // } else {
        //      dto.setOptions(new ArrayList<>());
        // }

        // 리뷰 정보: N+1 문제의 주범이 될 수 있음. DTO 프로젝션 시 서브쿼리나 집계 함수 사용 권장.
        // 또는 상품 ID 리스트를 모아 한 번의 쿼리로 리뷰 정보를 가져와서 매핑하는 방식.
        // 여기서는 간단히 표시 (실제로는 최적화 필수)
        Long reviewCount = reviewRepository.countByProductId(product.getId()); // N+1
        Double averageRating = reviewRepository.findAverageRatingByProductId(product.getId()).orElse(0.0); // N+1
        dto.setReviewCount(reviewCount);
        dto.setAverageRating(averageRating);
        
        dto.setInterestCount(0L); // 실제 관심 수 로직 필요

        return dto;
    }

    // Helper: ProductCategory -> ProductCategoryDTO.Response
    private ProductCategoryDTO.Response convertToCategoryResponse(ProductCategory category) {
        if (category == null) return null;
        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCreatedAt(category.getCreatedAt());
        // if (category.getParent() != null) {
        //     dto.setParent(convertToCategoryResponse(category.getParent()));
        // }
        return dto;
    }

    // Helper: User -> UserDTO.Response
    private UserDTO.Response convertToUserResponse(User user) {
        if (user == null) return null;
        UserDTO.Response dto = new UserDTO.Response();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail()); // 필요에 따라 추가 정보
        // ... (UserDTO.Response에 정의된 다른 필드들)
        return dto;
    }
    
    // Helper: ProductImage -> ProductImageDTO
    private ProductImageDTO convertToProductImageDTO(ProductImage image) {
        if (image == null) return null;
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setDisplayOrder(image.getDisplayOrder());
        dto.setCreatedAt(image.getCreatedAt());
        return dto;
    }

    // Helper: ProductOption -> ProductOptionDTO.Response
    private ProductOptionDTO.Response convertToOptionResponse(ProductOption option) {
        if (option == null) return null;
        ProductOptionDTO.Response dto = new ProductOptionDTO.Response();
        dto.setId(option.getId());
        dto.setSize(option.getSize());
        dto.setStockQuantity(option.getStockQuantity());
        dto.setAdditionalPrice(option.getAdditionalPrice());
        return dto;
    }


    public ProductDTO.Response getProduct(Integer productId) {
        // 상세 조회 시에는 모든 정보를 포함해야 하므로, Fetch Join이 필수적일 수 있음
        // 또는 ProductRepository에 @EntityGraph를 사용한 findById 정의
        return productRepository.findById(productId)
                .map(this::convertToResponseWithFetchedData) // N+1 주의
                .orElse(null);
    }

    public List<ProductDTO.Response> getProductsByCategory(Integer categoryId) {
        // 페이징 없이 카테고리별 모든 상품 조회 (N+1 주의)
        return productRepository.findByCategory_Id(categoryId).stream()
                .map(this::convertToResponseWithFetchedData) // N+1 주의
                .collect(Collectors.toList());
    }
    
    public List<ProductDTO.Response> getProductsBySeller(String userId) {
        // 페이징 없이 판매자별 모든 상품 조회 (N+1 주의)
        return productRepository.findByUser_Id(userId).stream()
                .map(this::convertToResponseWithFetchedData) // N+1 주의
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDTO.Response updateProduct(String userId, Integer productId, ProductDTO.UpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        if (!product.getUser().getId().equals(userId)) {
            throw new SecurityException("상품을 수정할 권한이 없습니다.");
        }

        if (request.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        
        // 이미지 업데이트 로직 (단순화된 예시, 실제로는 더 정교해야 함)
        if (request.getImageUrl() != null) {
            // 대표 이미지 변경 로직: 기존 대표 이미지 삭제 또는 플래그 변경, 새 이미지 추가 등
            // Product 엔티티의 imageUrl 필드를 직접 사용한다면 아래와 같이 간단히 할 수도 있음
             product.setImageUrl(request.getImageUrl());
        }
        // 추가 이미지, 삭제된 이미지 처리 로직 필요

        // 옵션 업데이트 로직
        if (request.getOptions() != null) {
            // 기존 옵션과 비교하여 변경/추가/삭제 로직 필요
        }
        
        Product updatedProduct = productRepository.save(product);
        return convertToResponseWithFetchedData(updatedProduct); // N+1 주의
    }

    @Transactional
    public void deleteProduct(String userId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        if (!product.getUser().getId().equals(userId)) {
            throw new SecurityException("상품을 삭제할 권한이 없습니다.");
        }
        productRepository.delete(product);
    }

    public List<ProductDTO.Response> searchProductsByKeyword(String keyword, String sortOption) {
        // 이 로직은 페이징이 없고, 애플리케이션 레벨에서 정렬하므로 데이터가 많을 때 비효율적일 수 있음.
        // findProductsWithFiltersAndSort와 유사하게 Specification과 Pageable을 사용하도록 개선하는 것이 좋음.
        List<Product> productsByName = productRepository.findByNameContainingIgnoreCase(keyword);
        List<ProductCategory> foundCategories = categoryRepository.findByNameContainingIgnoreCase(keyword);
        List<Product> productsByCategoryName = new ArrayList<>();
        if (!foundCategories.isEmpty()) {
            List<Integer> categoryIds = foundCategories.stream().map(ProductCategory::getId).collect(Collectors.toList());
            productsByCategoryName = productRepository.findByCategory_IdIn(categoryIds);
        }
    
        Set<Product> combinedProductSet = new HashSet<>(productsByName);
        combinedProductSet.addAll(productsByCategoryName);
    
        List<Product> sortedProducts = new ArrayList<>(combinedProductSet);
        switch (sortOption.toLowerCase()) {
            case "sales_volume_desc":
                sortedProducts.sort(Comparator.comparing(Product::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Product::getId));
                break;
            case "created_at_desc":
                sortedProducts.sort(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Product::getId));
                break;
            case "price_asc":
                sortedProducts.sort(Comparator.comparing(Product::getPrice).thenComparing(Product::getId));
                break;
            case "price_desc":
                sortedProducts.sort(Comparator.comparing(Product::getPrice, Comparator.reverseOrder()).thenComparing(Product::getId));
                break;
            case "musinsa_recommend":
            default:
                sortedProducts.sort(Comparator.comparing(Product::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Product::getId));
                break;
        }
    
        return sortedProducts.stream()
                .map(this::convertToResponseWithFetchedData) // N+1 주의
                .collect(Collectors.toList());
    }
}