package com.zorvyn.finance.service;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.entity.Role;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.AccessDeniedException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialRecordServiceTest {

    @Mock
    private FinancialRecordRepository recordRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FinancialRecordService recordService;

    private User adminUser;
    private User analystUser;
    private User viewerUser;
    private FinancialRecord sampleRecord;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId("admin-1");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);

        analystUser = new User();
        analystUser.setId("analyst-1");
        analystUser.setRole(Role.ANALYST);
        analystUser.setActive(true);

        viewerUser = new User();
        viewerUser.setId("viewer-1");
        viewerUser.setRole(Role.VIEWER);
        viewerUser.setActive(true);

        sampleRecord = new FinancialRecord();
        sampleRecord.setId("rec-1");
        sampleRecord.setAmount(5000.0);
        sampleRecord.setType(RecordType.INCOME);
        sampleRecord.setCategory("Salary");
        sampleRecord.setDate(LocalDate.of(2025, 3, 1));
        sampleRecord.setDeleted(false);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }
    

    @Test
    void createRecord_viewerCannotCreate_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.createRecord(sampleRecord))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createRecord_analystCannotCreate_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        assertThatThrownBy(() -> recordService.createRecord(sampleRecord))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createRecord_adminCanCreate_savesRecord() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.save(any())).thenReturn(sampleRecord);

        FinancialRecord result = recordService.createRecord(sampleRecord);
        assertThat(result.getId()).isEqualTo("rec-1");
        assertThat(sampleRecord.getUserId()).isEqualTo("admin-1");
        verify(recordRepository).save(sampleRecord);
    }

    // getAllRecords

    @Test
    void getAllRecords_viewerCannotAccess_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.getAllRecords())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAllRecords_analystCanRead_returnsList() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findByDeletedFalse()).thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.getAllRecords();
        assertThat(result).hasSize(1);
    }

    //deleteRecord (soft delete)

    @Test
    void deleteRecord_recordNotFound_throwsNotFound() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.deleteRecord("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRecord_adminWithValidId_setsDeletedTrue() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.findById("rec-1")).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        recordService.deleteRecord("rec-1");

        assertThat(sampleRecord.isDeleted()).isTrue();
        verify(recordRepository).save(sampleRecord);
        // should NOT call deleteById - it's a soft delete
        verify(recordRepository, never()).deleteById(any());
    }

    //filterRecords

    @Test
    void filterRecords_typeAndDateRange_usesCombinedQuery() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 3, 31);
        when(recordRepository.findByTypeAndDateBetweenAndDeletedFalse(RecordType.INCOME, from, to))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(RecordType.INCOME, null, from, to, null);
        assertThat(result).hasSize(1);
        verify(recordRepository).findByTypeAndDateBetweenAndDeletedFalse(RecordType.INCOME, from, to);
    }

    @Test
    void filterRecords_keywordSearch_usesCategoryKeyword() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findByCategoryContainingIgnoreCaseAndDeletedFalse("sal"))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(null, null, null, null, "sal");
        assertThat(result).hasSize(1);
        verify(recordRepository).findByCategoryContainingIgnoreCaseAndDeletedFalse("sal");
    }

    @Test
    void filterRecords_noParams_returnsAll() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findByDeletedFalse()).thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(null, null, null, null, null);
        assertThat(result).hasSize(1);
        verify(recordRepository).findByDeletedFalse();
    }

    @Test
    void filterRecords_viewer_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.filterRecords(null, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // getPaginated 

    @Test
    void getPaginated_viewer_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.getPaginated(0, 10))
                .isInstanceOf(AccessDeniedException.class);
    }
}
