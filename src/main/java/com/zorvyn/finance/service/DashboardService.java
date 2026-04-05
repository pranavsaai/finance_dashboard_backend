package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.dto.RecentActivityItem;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final UserService userService;

    public DashboardSummary getSummary() {

        // just verifying caller exists and is active — all roles can see dashboard
        userService.resolveCaller();

        Map<String, Double> totals = recordRepository.getIncomeExpenseTotals();
        double totalIncome = totals.getOrDefault("INCOME", 0.0);
        double totalExpense = totals.getOrDefault("EXPENSE", 0.0);

        Map<String, Map<String, Double>> categoryTotals = recordRepository.getCategoryTotals();
        Map<String, Double> monthlyTrends = recordRepository.getMonthlyTrends();

        // top 5 most recent non-deleted records
        List<RecentActivityItem> recentActivity = recordRepository
                .findTop5ByDeletedFalseOrderByDateDesc()
                .stream()
                .map(r -> new RecentActivityItem(
                        r.getId(),
                        r.getType(),
                        r.getAmount(),
                        r.getCategory(),
                        r.getDate()
                )).toList();

        return new DashboardSummary(
                totalIncome,
                totalExpense,
                totalIncome - totalExpense,
                categoryTotals,
                recentActivity,
                monthlyTrends
        );
    }
}