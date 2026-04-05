package com.zorvyn.finance.dto;

import com.zorvyn.finance.entity.RecordType;
import java.time.LocalDateTime;

public class RecentActivityItem {

    private String id;
    private RecordType type;
    private Double amount;
    private String category;
    private LocalDateTime date;

    public RecentActivityItem(String id, RecordType type, Double amount, String category, LocalDateTime date) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    // getters
    public String getId() { return id; }
    public RecordType getType() { return type; }
    public Double getAmount() { return amount; }
    public String getCategory() { return category; }
    public LocalDateTime getDate() { return date; }
}