package com.zorvyn.finance.dto;

import com.zorvyn.finance.entity.RecordType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe API response shape for financial records.
 *
 * 1.) The raw FinancialRecord entity was being returned directly from controllers,
 *     which exposed the internal userId field in every response. This DTO removes userId
 *     from API responses while keeping the entity clean for internal use.
 *
 * 2.) The 'deleted' field is already @JsonIgnore on the entity, but using a dedicated
 *     response DTO is the correct pattern — it decouples the persistence model from
 *     the API contract and prevents future fields from leaking accidentally.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRecordResponse {

    private String id;
    private Double amount;
    private RecordType type;
    private String category;
    private LocalDateTime date;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}