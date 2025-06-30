package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.SalesStatisticsDTO;
import com.shopsphere.shopsphere_web.entity.User;
import java.time.LocalDate;

public interface SalesService {
    SalesStatisticsDTO.Response getSalesStatistics(String userId, 
                                                 String category, 
                                                 String product, 
                                                 String timeRange, 
                                                 LocalDate startDate, 
                                                 LocalDate endDate);
}
