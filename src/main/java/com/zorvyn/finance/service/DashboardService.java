package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.repository.FinancialRecordRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final UserService userService;

    /**
     * Full dashboard summary - accessible by ALL roles (VIEWER, ANALYST, ADMIN).
     * Now uses MongoDB aggregation instead of Java streams.
     */
    public DashboardSummary getSummary() {

        userService.resolveCaller();

        double totalIncome = recordRepository.getTotalIncome();
        double totalExpense = recordRepository.getTotalExpense();

        Map<String, Double> categoryTotals = recordRepository.getCategoryTotals();
        Map<String, Double> monthlyTrends = recordRepository.getMonthlyTrends();
        List<Map<String, Object>> recentActivity = recordRepository
                .findTop5ByDeletedFalseOrderByDateDesc()
                .stream()
                .map(r -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", r.getId());
                    entry.put("type", r.getType());
                    entry.put("amount", r.getAmount());
                    entry.put("category", r.getCategory());
                    entry.put("date", r.getDate());
                    return entry;
                })
                .toList();

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