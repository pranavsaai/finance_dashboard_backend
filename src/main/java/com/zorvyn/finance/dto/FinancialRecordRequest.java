package com.zorvyn.finance.dto;

import com.zorvyn.finance.entity.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinancialRecordRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;

    @NotNull(message = "Type is required (INCOME or EXPENSE)")
    private RecordType type;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Date is required")
    private LocalDateTime date;

    private String notes;
}