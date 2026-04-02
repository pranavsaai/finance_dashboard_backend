package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinancialRecordRepository extends MongoRepository<FinancialRecord, String>,FinancialRecordCustomRepository {
    List<FinancialRecord> findByDeletedFalse();
    List<FinancialRecord> findByTypeAndDeletedFalse(RecordType type);
    List<FinancialRecord> findByCategoryAndDeletedFalse(String category);
    List<FinancialRecord> findByTypeAndCategoryAndDeletedFalse(RecordType type, String category);

    List<FinancialRecord> findByDateBetweenAndDeletedFalse(LocalDate from, LocalDate to);
    List<FinancialRecord> findByTypeAndDateBetweenAndDeletedFalse(RecordType type, LocalDate from, LocalDate to);
    List<FinancialRecord> findByCategoryAndDateBetweenAndDeletedFalse(String category, LocalDate from, LocalDate to);
    List<FinancialRecord> findByTypeAndCategoryAndDateBetweenAndDeletedFalse(RecordType type, String category, LocalDate from, LocalDate to);
    List<FinancialRecord> findByCategoryContainingIgnoreCaseOrNotesContainingIgnoreCaseAndDeletedFalse(String category,String notes);
    List<FinancialRecord> findByCategoryContainingIgnoreCaseAndDeletedFalse(String keyword);

    Page<FinancialRecord> findByDeletedFalse(Pageable pageable);

    List<FinancialRecord> findTop5ByDeletedFalseOrderByDateDesc();
}
