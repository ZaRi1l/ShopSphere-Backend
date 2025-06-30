package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class SalesStatisticsDTO {
    
    @Data
    public static class Request {
        private String category;
        private String product;
        private String timeRange;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class Response {
        private List<SalesData> salesData;
        private Summary summary;

        public Response(List<SalesData> salesData, Summary summary) {
            this.salesData = salesData;
            this.summary = summary;
        }
    }

    @Data
    public static class SalesData {
        private String label;  // 날짜, 월, 년도 등
        private Integer salesAmount;
        private Integer orderCount;
        private Integer productCount;
    }

    @Data
    public static class Summary {
        private Integer totalSalesAmount;
        private Integer totalOrderCount;
        private Integer totalProductCount;
        private Map<String, Integer> salesByCategory;
        private Map<String, Integer> salesByProduct;
    }
}
