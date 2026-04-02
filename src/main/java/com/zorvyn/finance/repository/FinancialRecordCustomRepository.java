package com.zorvyn.finance.repository;

import java.util.Map;

public interface FinancialRecordCustomRepository {

    double getTotalIncome();

    double getTotalExpense();

    Map<String, Double> getCategoryTotals();

    Map<String, Double> getMonthlyTrends();
}