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
        record.setUserId(caller.getId()); // tag record with who created it
        return recordRepository.save(record);
    }

    // ANALYST + ADMIN
    public List<FinancialRecord> getAllRecords() {
        User caller = userService.resolveCaller();
        assertNotViewer(caller);
        return recordRepository.findAll();
    }

    // ADMIN only
    public FinancialRecord updateRecord(String id, FinancialRecord updated) {
        User caller = userService.resolveCaller();
        assertAdmin(caller);

        FinancialRecord existing = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        existing.setAmount(updated.getAmount());
        existing.setType(updated.getType());
        existing.setCategory(updated.getCategory());
        existing.setDate(updated.getDate());
        existing.setNotes(updated.getNotes());

        return recordRepository.save(existing);
    }

    // ADMIN only
    public void deleteRecord(String id) {
        User caller = userService.resolveCaller();
        assertAdmin(caller);

        if (!recordRepository.existsById(id)) {
            throw new ResourceNotFoundException("Record not found with id: " + id);
        }
        recordRepository.deleteById(id);
    }

    /** Updates did regarding records filters:
     * Filter records by type, category, and/or date range.
     * All parameters are optional and fully combinable:
     *   - type + category + date range → all three applied
     *   - type + date range           → type and date range applied
     *   - category + date range       → category and date range applied
     *   - type + category             → both applied
     *   - any single param            → that param alone
     *   - no params                   → returns all records
     * ANALYST + ADMIN only.
     */
    public List<FinancialRecord> filterRecords(RecordType type, String category,
                                               LocalDate from, LocalDate to) {
        User caller = userService.resolveCaller();
        assertNotViewer(caller);

        boolean hasType     = type != null;
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDateRange = from != null && to != null;

        if (hasType && hasCategory && hasDateRange) {
            return recordRepository.findByTypeAndCategoryAndDateBetween(type, category, from, to);
        }
        if (hasType && hasDateRange) {
            return recordRepository.findByTypeAndDateBetween(type, from, to);
        }
        if (hasCategory && hasDateRange) {
            return recordRepository.findByCategoryAndDateBetween(category, from, to);
        }
        if (hasType && hasCategory) {
            return recordRepository.findByTypeAndCategory(type, category);
        }
        if (hasDateRange) {
            return recordRepository.findByDateBetween(from, to);
        }
        if (hasType) {
            return recordRepository.findByType(type);
        }
        if (hasCategory) {
            return recordRepository.findByCategory(category);
        }

        return recordRepository.findAll();
    }

    // paginated listing - no role restriction beyond being a valid user
    public List<FinancialRecord> getPaginated(int page, int size) {
        userService.resolveCaller(); // just validates the header is present
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        return recordRepository.findAll(pageable).getContent();
    }

    // Role assertion helpers 

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
