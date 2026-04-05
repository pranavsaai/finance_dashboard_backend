package com.zorvyn.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private double totalIncome;
    private double totalExpense;
    private double netBalance;
    private Map<String, Map<String, Double>> categoryTotals;
    private List<RecentActivityItem> recentActivity;
    private Map<String, Double> monthlyTrends;
}