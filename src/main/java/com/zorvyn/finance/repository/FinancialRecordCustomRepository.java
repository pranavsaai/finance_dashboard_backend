package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FinancialRecordCustomRepository {

    double getTotalIncome();

    double getTotalExpense();

    Map<String, Map<String, Double>> getCategoryTotals();

    Map<String, Double> getMonthlyTrends();

    List<FinancialRecord> filterDynamic(
            RecordType type,
            String category,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            String userId
    );
}