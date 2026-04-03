package com.zorvyn.finance.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class FinancialRecordCustomRepositoryImpl implements FinancialRecordCustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public double getTotalIncome() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("type").is("INCOME").and("deleted").is(false)),
                Aggregation.group().sum("amount").as("total")
        );

        Map<String, Object> result = mongoTemplate.aggregate(agg, "records", Map.class)
                .getUniqueMappedResult();

        return result != null ? ((Number) result.get("total")).doubleValue() : 0;
    }

    @Override
    public double getTotalExpense() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("type").is("EXPENSE").and("deleted").is(false)),
                Aggregation.group().sum("amount").as("total")
        );

        Map<String, Object> result = mongoTemplate.aggregate(agg, "records", Map.class)
                .getUniqueMappedResult();

        return result != null ? ((Number) result.get("total")).doubleValue() : 0;
    }

    @Override
    public Map<String, Double> getCategoryTotals() {

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deleted").is(false)),
                Aggregation.group("category").sum("amount").as("total")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>)(List<?>) 
                mongoTemplate.aggregate(agg, "records", Map.class).getMappedResults();

        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> r : results) {
            map.put((String) r.get("_id"), ((Number) r.get("total")).doubleValue());
        }

        return map;
    }

    @Override
    public Map<String, Double> getMonthlyTrends() {

        Aggregation agg = Aggregation.newAggregation(

                Aggregation.match(Criteria.where("deleted").is(false)),

                Aggregation.project()
                        .and(DateOperators.DateToString.dateOf("date").toString("%Y")
                                .withTimezone(DateOperators.Timezone.valueOf("Asia/Kolkata"))).as("year")
                        .and(DateOperators.DateToString.dateOf("date").toString("%m")
                                .withTimezone(DateOperators.Timezone.valueOf("Asia/Kolkata"))).as("month")
                        .and("amount").as("amount")
                        .and("type").as("type"),

                Aggregation.group("year", "month")
                        .sum(ConditionalOperators.when(Criteria.where("type").is("INCOME"))
                                .thenValueOf("amount")
                                .otherwise(ArithmeticOperators.Multiply.valueOf("amount").multiplyBy(-1)))
                        .as("total"),

                Aggregation.sort(Sort.by("year").ascending().and(Sort.by("month").ascending()))
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>)(List<?>) 
                mongoTemplate.aggregate(agg, "records", Map.class).getMappedResults();

        Map<String, Double> map = new LinkedHashMap<>();

        for (Map<String, Object> r : results) {

            @SuppressWarnings("unchecked")
            Map<String, Object> id = (Map<String, Object>) r.get("_id");

            String year = (String) id.get("year");
            String month = (String) id.get("month");

            String key = year + "-" + month;

            map.put(key, ((Number) r.get("total")).doubleValue());
        }

        return map;
    }
}