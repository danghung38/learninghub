package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.RevenueDetailResponse;
import org.springframework.stereotype.Component;

@Component
public class RevenueMapper {

    public RevenueDetailResponse toMonthlyDetail(String month, Integer year, Long revenue) {
        return RevenueDetailResponse.builder()
                .month(month)
                .year(year)
                .revenue(revenue)
                .build();
    }

    public RevenueDetailResponse toWeeklyDetail(String week, String month, Integer year, Long revenue) {
        return RevenueDetailResponse.builder()
                .week(week)
                .month(month)
                .year(year)
                .revenue(revenue)
                .build();
    }
}