package com.zorvyn.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class DashboardSummary {
    private double totalIncome;
    private double totalExpense;
    private double netBalance;
    private Map<String, Double> categoryTotals;
    private List<Map<String, Object>> recentActivity;
    private Map<String, Double> monthlyTrends;
}
