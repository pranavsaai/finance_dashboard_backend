package com.zorvyn.finance.controller;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.service.FinancialRecordService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    public FinancialRecordController(FinancialRecordService recordService) {
        this.recordService = recordService;
    }

    // ADMIN only - caller resolved from X-User-Id header
    @PostMapping
    public ResponseEntity<FinancialRecord> create(@Valid @RequestBody FinancialRecord record) {
        return new ResponseEntity<>(recordService.createRecord(record), HttpStatus.CREATED);
    }

    // ANALYST + ADMIN
    @GetMapping
    public List<FinancialRecord> getAll() {
        return recordService.getAllRecords();
    }

    // ADMIN only
    @PutMapping("/{id}")
    public FinancialRecord update(@PathVariable String id,
                                  @Valid @RequestBody FinancialRecord record) {
        return recordService.updateRecord(id, record);
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }

    // ANALYST + ADMIN - filter by type, category, and/or date range
    @GetMapping("/filter")
    public List<FinancialRecord> filter(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return recordService.filterRecords(type, category, from, to);
    }

    // all authenticated users
    @GetMapping("/paginated")
    public List<FinancialRecord> getPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return recordService.getPaginated(page, size);
    }
}
