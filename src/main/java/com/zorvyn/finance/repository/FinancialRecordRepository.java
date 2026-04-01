package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface FinancialRecordRepository extends MongoRepository<FinancialRecord, String> {

    List<FinancialRecord> findByType(RecordType type);

    List<FinancialRecord> findByCategory(String category);

    List<FinancialRecord> findByTypeAndCategory(RecordType type, String category);

    List<FinancialRecord> findByDateBetween(LocalDate from, LocalDate to);

    List<FinancialRecord> findByTypeAndDateBetween(RecordType type, LocalDate from, LocalDate to);

    List<FinancialRecord> findByCategoryAndDateBetween(String category, LocalDate from, LocalDate to);

    List<FinancialRecord> findByTypeAndCategoryAndDateBetween(RecordType type, String category, LocalDate from, LocalDate to);

    // for paginated listing
    Page<FinancialRecord> findAll(Pageable pageable);

    // recent N records sorted by date descending
    List<FinancialRecord> findTop5ByOrderByDateDesc();
}
