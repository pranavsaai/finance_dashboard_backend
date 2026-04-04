package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.FinancialRecordRequest;
import com.zorvyn.finance.dto.PageResponse;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.service.FinancialRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FinancialRecord> create(@Valid @RequestBody FinancialRecordRequest request) {
        return new ResponseEntity<>(recordService.createRecord(request), HttpStatus.CREATED);
    }

    // ANALYST + ADMIN
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping
    public List<FinancialRecord> getAll() {
        return recordService.getAllRecords();
    }

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public FinancialRecord update(@PathVariable String id,
                                  @Valid @RequestBody FinancialRecordRequest request) {
        return recordService.updateRecord(id, request);
    }

    // ADMIN only - soft deletes the record (sets deleted=true, not removed from DB)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }

    // ANALYST + ADMIN
    // supports: type, category, from/to date range, and keyword search (all optional, combinable)
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/filter")
    public List<FinancialRecord> filter(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search) {
        return recordService.filterRecords(type, category, from, to, search);
    }

    // ANALYST + ADMIN only
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/paginated")
    public PageResponse<FinancialRecord> getPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return recordService.getPaginated(page, size);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/{id}")
    public FinancialRecord getById(@PathVariable String id) {
        return recordService.getRecordById(id);
    }
}