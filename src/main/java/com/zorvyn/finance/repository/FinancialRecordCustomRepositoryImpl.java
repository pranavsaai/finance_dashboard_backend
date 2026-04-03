package com.zorvyn.finance.repository;

import lombok.RequiredArgsConstructor;
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

        Map result = mongoTemplate.aggregate(agg, "records", Map.class)
                .getUniqueMappedResult();

        return result != null ? ((Number) result.get("total")).doubleValue() : 0;
    }

    @Override
    public double getTotalExpense() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("type").is("EXPENSE").and("deleted").is(false)),
                Aggregation.group().sum("amount").as("total")
        );

        Map result = mongoTemplate.aggregate(agg, "records", Map.class)
                .getUniqueMappedResult();

        return result != null ? ((Number) result.get("total")).doubleValue() : 0;
    }

    @Override
    public Map<String, Double> getCategoryTotals() {

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deleted").is(false)),
                Aggregation.group("category").sum("amount").as("total")
        );

        List<Map> results = mongoTemplate.aggregate(agg, "records", Map.class)
                .getMappedResults();

        Map<String, Double> map = new HashMap<>();

        for (Map r : results) {
            map.put((String) r.get("_id"), ((Number) r.get("total")).doubleValue());
        }

        return map;
    }

    @Override
    public Map<String, Double> getMonthlyTrends() {

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.project()
                        .andExpression("year(date)").as("year")
                        .andExpression("month(date)").as("month")
                        .and("amount").as("amount")
                        .and("type").as("type"),

                Aggregation.group("year", "month")
                        .sum(
                                ConditionalOperators.when(Criteria.where("type").is("INCOME"))
                                        .thenValueOf("amount")
                                        .otherwise(
                                                ArithmeticOperators.Multiply.valueOf("amount").multiplyBy(-1)
                                        )
                        ).as("total")
        );

        List<Map> results = mongoTemplate.aggregate(agg, "records", Map.class)
                .getMappedResults();

        Map<String, Double> map = new TreeMap<>();

        for (Map r : results) {
            Map id = (Map) r.get("_id");
            String key = id.get("year") + "-" + String.format("%02d", id.get("month"));
            map.put(key, ((Number) r.get("total")).doubleValue());
        }

        return map;
    }
}
