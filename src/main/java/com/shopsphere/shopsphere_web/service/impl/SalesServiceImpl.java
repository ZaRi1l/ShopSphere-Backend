package com.shopsphere.shopsphere_web.service.impl;

import com.shopsphere.shopsphere_web.dto.SalesStatisticsDTO;
import com.shopsphere.shopsphere_web.entity.OrderItem;
import com.shopsphere.shopsphere_web.entity.Product;
import com.shopsphere.shopsphere_web.repository.OrderItemRepository;
import com.shopsphere.shopsphere_web.repository.ProductRepository;
import com.shopsphere.shopsphere_web.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

        private final OrderItemRepository orderItemRepository;
        private final ProductRepository productRepository;

        @Override
        @Transactional(readOnly = true)
        public SalesStatisticsDTO.Response getSalesStatistics(String userId,
                        String category,
                        String product,
                        String timeRange,
                        LocalDate startDate,
                        LocalDate endDate) {

                System.out.println("Getting sales statistics for user: " + userId);
                System.out.println("Parameters - category: " + category + ", product: " + product +
                                ", timeRange: " + timeRange + ", startDate: " + startDate + ", endDate: " + endDate);

                // 날짜 범위 설정
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);

                // 판매자의 상품 ID 목록 조회 (컬렉션 페치 조인 없이 기본 조회)
                List<Integer> sellerProductIds = productRepository.findByUserId(userId).stream()
                                .map(Product::getId)
                                .collect(Collectors.toList());

                // 주문 상품 조회 (카테고리 필터 적용)
                List<OrderItem> orderItems;
                if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                    System.out.println("Filtering by category: " + category);
                    orderItems = orderItemRepository.findByProductIdInAndCreatedAtBetweenAndCategory(
                            sellerProductIds, startDateTime, endDateTime, category);
                } else {
                    orderItems = orderItemRepository.findByProductIdInAndCreatedAtBetween(
                            sellerProductIds, startDateTime, endDateTime);
                }

                // 필요한 경우에만 상품 정보 로드
                if (!orderItems.isEmpty()) {
                    // 상품 정보만 로드 (이미지와 옵션은 필요할 때만 로드)
                    orderItems.forEach(oi -> Hibernate.initialize(oi.getProduct()));
                }

                // 상품 필터 적용
                if (product != null && !product.isEmpty() && !"all".equals(product)) {
                        orderItems = orderItems.stream()
                                        .filter(item -> product.equals(item.getProduct().getName()))
                                        .collect(Collectors.toList());
                }

                // 시간 범위에 따른 데이터 그룹화
                List<SalesStatisticsDTO.SalesData> salesData;

                if ("daily".equals(timeRange)) {
                        Map<LocalDate, List<OrderItem>> dailySales = orderItems.stream()
                                        .collect(Collectors.groupingBy(item -> item.getCreatedAt().toLocalDate()));

                        salesData = dailySales.entrySet().stream()
                                        .map(entry -> createSalesData(
                                                        entry.getKey().toString(),
                                                        entry.getValue()))
                                        .sorted(Comparator.comparing(SalesStatisticsDTO.SalesData::getLabel))
                                        .collect(Collectors.toList());
                } else if ("weekly".equals(timeRange)) {
                        // 주간 그룹화 (연도-주차 형식으로 표시)
                        Map<String, List<OrderItem>> weeklySales = orderItems.stream()
                                        .collect(Collectors.groupingBy(item -> {
                                            LocalDate date = item.getCreatedAt().toLocalDate();
                                            int year = date.getYear();
                                            int week = date.get(WeekFields.of(Locale.KOREA).weekOfWeekBasedYear());
                                            return String.format("%d-%02d", year, week);
                                        }));

                        salesData = weeklySales.entrySet().stream()
                                        .map(entry -> createSalesData(
                                                        entry.getKey() + "주차",
                                                        entry.getValue()))
                                        .sorted(Comparator.comparing(SalesStatisticsDTO.SalesData::getLabel))
                                        .collect(Collectors.toList());
                } else if ("monthly".equals(timeRange)) {
                        Map<YearMonth, List<OrderItem>> monthlySales = orderItems.stream()
                                        .collect(Collectors.groupingBy(item -> YearMonth.from(item.getCreatedAt())));

                        salesData = monthlySales.entrySet().stream()
                                        .map(entry -> createSalesData(
                                                        entry.getKey().getYear() + "-" + 
                                                        String.format("%02d", entry.getKey().getMonthValue()),
                                                        entry.getValue()))
                                        .sorted(Comparator.comparing(SalesStatisticsDTO.SalesData::getLabel))
                                        .collect(Collectors.toList());
                } else if ("yearly".equals(timeRange)) {
                        Map<Integer, List<OrderItem>> yearlySales = orderItems.stream()
                                        .collect(Collectors.groupingBy(item -> item.getCreatedAt().getYear()));

                        salesData = yearlySales.entrySet().stream()
                                        .map(entry -> createSalesData(
                                                        String.valueOf(entry.getKey()),
                                                        entry.getValue()))
                                        .sorted(Comparator.comparing(SalesStatisticsDTO.SalesData::getLabel))
                                        .collect(Collectors.toList());
                } else {
                        // 기본은 전체 기간으로 집계
                        SalesStatisticsDTO.SalesData allTimeData = createSalesData("전체", orderItems);
                        salesData = List.of(allTimeData);
                }

                // 요약 정보 생성
                SalesStatisticsDTO.Summary summary = createSummary(orderItems);

                return new SalesStatisticsDTO.Response(salesData, summary);
        }

        private SalesStatisticsDTO.SalesData createSalesData(String label, List<OrderItem> items) {
                int totalSales = items.stream()
                                .mapToInt(item -> item.getPrice() * item.getQuantity())
                                .sum();

                long orderCount = items.stream()
                                .map(OrderItem::getOrder)
                                .distinct()
                                .count();

                int productCount = items.stream()
                                .mapToInt(OrderItem::getQuantity)
                                .sum();

                SalesStatisticsDTO.SalesData data = new SalesStatisticsDTO.SalesData();
                data.setLabel(label);
                data.setSalesAmount(totalSales);
                data.setOrderCount((int) orderCount);
                data.setProductCount(productCount);

                return data;
        }

        private SalesStatisticsDTO.Summary createSummary(List<OrderItem> items) {
                SalesStatisticsDTO.Summary summary = new SalesStatisticsDTO.Summary();

                // 총 매출액
                int totalSales = items.stream()
                                .mapToInt(item -> item.getPrice() * item.getQuantity())
                                .sum();

                // 총 주문 건수
                long orderCount = items.stream()
                                .map(OrderItem::getOrder)
                                .distinct()
                                .count();

                // 총 판매 상품 수
                int productCount = items.stream()
                                .mapToInt(OrderItem::getQuantity)
                                .sum();

                // 카테고리별 매출
                Map<String, Integer> salesByCategory = items.stream()
                                .collect(Collectors.groupingBy(
                                                item -> item.getProduct().getCategory().toString(), // Convert category
                                                                                                    // to String
                                                Collectors.summingInt(item -> item.getPrice() * item.getQuantity())));

                // 상품별 매출
                Map<String, Integer> salesByProduct = items.stream()
                                .collect(Collectors.groupingBy(
                                                item -> item.getProduct().getName(),
                                                Collectors.summingInt(item -> item.getPrice() * item.getQuantity())));

                summary.setTotalSalesAmount(totalSales);
                summary.setTotalOrderCount((int) orderCount);
                summary.setTotalProductCount(productCount);
                summary.setSalesByCategory(salesByCategory);
                summary.setSalesByProduct(salesByProduct);

                return summary;
        }
}
