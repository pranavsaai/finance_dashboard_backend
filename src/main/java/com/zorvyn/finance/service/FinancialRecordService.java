package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.PageResponse;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.entity.Role;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.AccessDeniedException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserService userService;

    // ADMIN only
    public FinancialRecord createRecord(FinancialRecord record) {
        User caller = userService.resolveCaller();
        assertAdmin(caller);
        record.setUserId(caller.getId());
        record.setDeleted(false);
        return recordRepository.save(record);
    }

    // ANALYST + ADMIN
    public List<FinancialRecord> getAllRecords() {
        User caller = userService.resolveCaller();
        assertNotViewer(caller);
        return recordRepository.findByDeletedFalse();
    }

    // ADMIN only
    public FinancialRecord updateRecord(String id, FinancialRecord updated) {
        User caller = userService.resolveCaller();
        assertAdmin(caller);

        FinancialRecord existing = recordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        existing.setAmount(updated.getAmount());
        existing.setType(updated.getType());
        existing.setCategory(updated.getCategory());
        existing.setDate(updated.getDate());
        existing.setNotes(updated.getNotes());

        return recordRepository.save(existing);
    }

    // ADMIN only (soft delete)
    public void deleteRecord(String id) {
        User caller = userService.resolveCaller();
        assertAdmin(caller);

        FinancialRecord record = recordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        record.setDeleted(true);
        recordRepository.save(record);
    }

    public List<FinancialRecord> filterRecords(RecordType type, String category,
                                               LocalDate from, LocalDate to,
                                               String search) {

        User caller = userService.resolveCaller();
        assertNotViewer(caller);

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        if (from != null) {
            fromDateTime = from.atStartOfDay();
        }
        if (to != null) {
            toDateTime = to.atTime(LocalTime.MAX);
        }

        // Search priority
        if (search != null && !search.isBlank()) {
            return recordRepository.search(search.trim());
        }

        boolean hasType = type != null;
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDateRange = fromDateTime != null && toDateTime != null;

        if (hasType && hasCategory && hasDateRange) {
            return recordRepository.findByTypeAndCategoryAndDateBetweenAndDeletedFalse(
                    type, category, fromDateTime, toDateTime);
        }
        if (hasType && hasDateRange) {
            return recordRepository.findByTypeAndDateBetweenAndDeletedFalse(
                    type, fromDateTime, toDateTime);
        }
        if (hasCategory && hasDateRange) {
            return recordRepository.findByCategoryAndDateBetweenAndDeletedFalse(
                    category, fromDateTime, toDateTime);
        }
        if (hasType && hasCategory) {
            return recordRepository.findByTypeAndCategoryAndDeletedFalse(type, category);
        }
        if (hasDateRange) {
            return recordRepository.findByDateBetweenAndDeletedFalse(fromDateTime, toDateTime);
        }
        if (hasType) {
            return recordRepository.findByTypeAndDeletedFalse(type);
        }
        if (hasCategory) {
            return recordRepository.findByCategoryAndDeletedFalse(category);
        }

        return recordRepository.findByDeletedFalse();
    }

    public PageResponse<FinancialRecord> getPaginated(int page, int size) {

        User caller = userService.resolveCaller();
        assertNotViewer(caller);

        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "date")
        );
        Page<FinancialRecord> result = recordRepository.findByDeletedFalse(pageable);
        return new PageResponse<>(
                result.getContent(),
                page,
                size,
                result.getTotalElements()
        );
    }

    private void assertAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }

    private void assertNotViewer(User user) {
        if (user.getRole() == Role.VIEWER) {
            throw new AccessDeniedException("VIEWER role does not have access to records");
        }
    }
}