package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;  
import java.util.List;

@Repository
public interface FinancialRecordRepository extends MongoRepository<FinancialRecord, String>, FinancialRecordCustomRepository {

    List<FinancialRecord> findByDeletedFalse();
    List<FinancialRecord> findByTypeAndDeletedFalse(RecordType type);
    List<FinancialRecord> findByCategoryAndDeletedFalse(String category);
    List<FinancialRecord> findByTypeAndCategoryAndDeletedFalse(RecordType type, String category);
    List<FinancialRecord> findByDateBetweenAndDeletedFalse(LocalDateTime from, LocalDateTime to);

    List<FinancialRecord> findByTypeAndDateBetweenAndDeletedFalse(RecordType type, LocalDateTime from, LocalDateTime to);
    List<FinancialRecord> findByCategoryAndDateBetweenAndDeletedFalse(String category, LocalDateTime from, LocalDateTime to);
    List<FinancialRecord> findByTypeAndCategoryAndDateBetweenAndDeletedFalse(RecordType type, String category, LocalDateTime from, LocalDateTime to);

    Page<FinancialRecord> findByDeletedFalse(Pageable pageable);
    List<FinancialRecord> findTop5ByDeletedFalseOrderByDateDesc();

    @Query("{ deleted: false, $or: [ { category: { $regex: ?0, $options: 'i' } }, { notes: { $regex: ?0, $options: 'i' } } ] }")
    List<FinancialRecord> search(String keyword);
}