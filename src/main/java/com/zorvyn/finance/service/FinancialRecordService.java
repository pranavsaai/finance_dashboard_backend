package com.zorvyn.finance.service;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.entity.Role;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.AccessDeniedException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserService userService;

    public FinancialRecordService(FinancialRecordRepository recordRepository,
                                  UserService userService) {
        this.recordRepository = recordRepository;
        this.userService = userService;
    }

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

    // ADMIN only - soft delete: marks the record as deleted instead of removing it
    public void deleteRecord(String id) {
        User caller = userService.resolveCaller();
        assertAdmin(caller);

        FinancialRecord record = recordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        record.setDeleted(true);
        recordRepository.save(record);
    }

    /**
     * Filter records by any combination of type, category, date range, or keyword search.
     * All params are optional. When 'search' is provided it does a keyword match on category.
     * ANALYST + ADMIN only.
     */
    public List<FinancialRecord> filterRecords(RecordType type, String category,
                                               LocalDate from, LocalDate to,
                                               String search) {
        User caller = userService.resolveCaller();
        assertNotViewer(caller);

        // keyword search takes priority when provided
        if (search != null && !search.isBlank()) {
            return recordRepository.findByCategoryContainingIgnoreCaseAndDeletedFalse(search.trim());
        }

        boolean hasType      = type != null;
        boolean hasCategory  = category != null && !category.isBlank();
        boolean hasDateRange = from != null && to != null;

        if (hasType && hasCategory && hasDateRange) {
            return recordRepository.findByTypeAndCategoryAndDateBetweenAndDeletedFalse(type, category, from, to);
        }
        if (hasType && hasDateRange) {
            return recordRepository.findByTypeAndDateBetweenAndDeletedFalse(type, from, to);
        }
        if (hasCategory && hasDateRange) {
            return recordRepository.findByCategoryAndDateBetweenAndDeletedFalse(category, from, to);
        }
        if (hasType && hasCategory) {
            return recordRepository.findByTypeAndCategoryAndDeletedFalse(type, category);
        }
        if (hasDateRange) {
            return recordRepository.findByDateBetweenAndDeletedFalse(from, to);
        }
        if (hasType) {
            return recordRepository.findByTypeAndDeletedFalse(type);
        }
        if (hasCategory) {
            return recordRepository.findByCategoryAndDeletedFalse(category);
        }

        return recordRepository.findByDeletedFalse();
    }

    // any authenticated user
    public List<FinancialRecord> getPaginated(int page, int size) {
        userService.resolveCaller();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        return recordRepository.findByDeletedFalse(pageable).getContent();
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
