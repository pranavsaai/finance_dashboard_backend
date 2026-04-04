package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.FinancialRecordRequest;
import com.zorvyn.finance.dto.FinancialRecordResponse;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FinancialRecordResponse> create(@Valid @RequestBody FinancialRecordRequest request) {
        return new ResponseEntity<>(toResponse(recordService.createRecord(request)), HttpStatus.CREATED);
    }

    // ANALYST + ADMIN
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping
    public List<FinancialRecordResponse> getAll() {
        return recordService.getAllRecords()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public FinancialRecordResponse update(@PathVariable String id,
                                          @Valid @RequestBody FinancialRecordRequest request) {
        return toResponse(recordService.updateRecord(id, request));
    }

    // ADMIN only — soft deletes the record (sets deleted=true, not removed from DB)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }

    // ANALYST + ADMIN
    // supports: type, category, from/to date range, keyword search (all optional, combinable)
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/filter")
    public List<FinancialRecordResponse> filter(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search) {
        return recordService.filterRecords(type, category, from, to, search)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ANALYST + ADMIN — paginated listing (max 100 per page)
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/paginated")
    public PageResponse<FinancialRecordResponse> getPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<FinancialRecord> raw = recordService.getPaginated(page, size);
        List<FinancialRecordResponse> mapped = raw.getData()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(mapped, raw.getPage(), raw.getSize(), raw.getTotal());
    }

    // ANALYST + ADMIN
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/{id}")
    public FinancialRecordResponse getById(@PathVariable String id) {
        return toResponse(recordService.getRecordById(id));
    }

    /**
     * Maps a FinancialRecord entity to a safe API response.
     * FIX: Excludes userId from API responses — it is an internal field
     * that has no meaning to API consumers.
     */
    private FinancialRecordResponse toResponse(FinancialRecord r) {
        return new FinancialRecordResponse(
                r.getId(),
                r.getAmount(),
                r.getType(),
                r.getCategory(),
                r.getDate(),
                r.getNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}