package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final UserService userService;

    public DashboardService(FinancialRecordRepository recordRepository,
                            UserService userService) {
        this.recordRepository = recordRepository;
        this.userService = userService;
    }

    /**
     * Full dashboard summary - accessible by ALL roles (VIEWER, ANALYST, ADMIN).
     * Returns income, expense, balance, category totals, recent activity, and monthly trends.
     */
    public DashboardSummary getSummary() {
        userService.resolveCaller(); // any authenticated user can view the dashboard

        List<FinancialRecord> all = recordRepository.findByDeletedFalse();

        double totalIncome = all.stream()
                .filter(r -> r.getType() == RecordType.INCOME)
                .mapToDouble(FinancialRecord::getAmount)
                .sum();

        double totalExpense = all.stream()
                .filter(r -> r.getType() == RecordType.EXPENSE)
                .mapToDouble(FinancialRecord::getAmount)
                .sum();

        Map<String, Double> categoryTotals = all.stream()
                .collect(Collectors.groupingBy(
                        FinancialRecord::getCategory,
                        Collectors.summingDouble(FinancialRecord::getAmount)
                ));

        List<Map<String, Object>> recentActivity = recordRepository.findTop5ByDeletedFalseOrderByDateDesc()
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
                .collect(Collectors.toList());

        Map<String, Double> monthlyTrends = buildMonthlyTrends(all);

        return new DashboardSummary(
                totalIncome,
                totalExpense,
                totalIncome - totalExpense,
                categoryTotals,
                recentActivity,
                monthlyTrends
        );
    }

    /**
     * Aggregates net amount (income - expense) grouped by year-month.
     * Example key: "2025-03"
     */
    private Map<String, Double> buildMonthlyTrends(List<FinancialRecord> records) {
        Map<String, Double> trends = new TreeMap<>(); // TreeMap keeps months in order

        for (FinancialRecord r : records) {
            if (r.getDate() == null) continue;

            String month = r.getDate().getYear() + "-"
                    + String.format("%02d", r.getDate().getMonthValue());

            double value = r.getType() == RecordType.INCOME ? r.getAmount() : -r.getAmount();
            trends.merge(month, value, Double::sum);
        }

        return trends;
    }
}
