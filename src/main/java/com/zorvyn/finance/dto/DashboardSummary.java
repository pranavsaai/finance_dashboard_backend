package com.zorvyn.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregated dashboard response returned by GET /api/dashboard/summary.
 *
 * FIX: categoryTotals changed from Map<String, Double> to Map<String, Map<String, Double>>
 * to separate INCOME and EXPENSE totals per category.
 *
 * Example response shape:
 * {
 *   "totalIncome": 150000.0,
 *   "totalExpense": 45000.0,
 *   "netBalance": 105000.0,
 *   "categoryTotals": {
 *     "Salary":    { "INCOME": 150000.0 },
 *     "Utilities": { "EXPENSE": 12000.0 },
 *     "Freelance": { "INCOME": 20000.0, "EXPENSE": 500.0 }
 *   },
 *   "recentActivity": [ ... ],
 *   "monthlyTrends": { "2025-01": 55000.0, "2025-02": -12000.0 }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private double totalIncome;

    private double totalExpense;

    private double netBalance;

    private Map<String, Map<String, Double>> categoryTotals;
    
    private List<Map<String, Object>> recentActivity;

    /**
     * Net balance per calendar month (YYYY-MM).
     * Positive = net income that month, negative = net expense that month.
     * Timezone: IST (Asia/Kolkata).
     */
    private Map<String, Double> monthlyTrends;
}