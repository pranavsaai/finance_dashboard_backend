package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.FinancialRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialRecordRepository extends MongoRepository<FinancialRecord, String>, FinancialRecordCustomRepository {

    // Used by FinancialRecordService.getAllRecords()
    List<FinancialRecord> findByDeletedFalse();

    // Used by FinancialRecordService.getPaginated()
    Page<FinancialRecord> findByDeletedFalse(Pageable pageable);

    // Used by DashboardService.getSummary() — recent activity
    List<FinancialRecord> findTop5ByDeletedFalseOrderByDateDesc();
}