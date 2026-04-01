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
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    //createRecord

    @Test
    void createRecord_viewerCannotCreate_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);

        assertThatThrownBy(() -> recordService.createRecord(sampleRecord))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void createRecord_analystCannotCreate_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        assertThatThrownBy(() -> recordService.createRecord(sampleRecord))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void createRecord_adminCanCreate_savesRecord() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.save(any())).thenReturn(sampleRecord);

        FinancialRecord result = recordService.createRecord(sampleRecord);
        assertThat(result.getId()).isEqualTo("rec-1");
        assertThat(sampleRecord.getUserId()).isEqualTo("admin-1"); // tagged with creator
        verify(recordRepository).save(sampleRecord);
    }

    //getAllRecords

    @Test
    void getAllRecords_viewerCannotAccess_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);

        assertThatThrownBy(() -> recordService.getAllRecords())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("VIEWER");
    }

    @Test
    void getAllRecords_analystCanRead_returnsList() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findAll()).thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.getAllRecords();
        assertThat(result).hasSize(1);
    }


    @Test
    void deleteRecord_recordNotFound_throwsNotFound() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.existsById("bad-id")).thenReturn(false);

        assertThatThrownBy(() -> recordService.deleteRecord("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRecord_adminWithValidId_deletesSuccessfully() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.existsById("rec-1")).thenReturn(true);

        recordService.deleteRecord("rec-1");
        verify(recordRepository).deleteById("rec-1");
    }


    @Test
    void filterRecords_typeAndDateRange_usesCombinedQuery() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 3, 31);
        when(recordRepository.findByTypeAndDateBetween(RecordType.INCOME, from, to))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(RecordType.INCOME, null, from, to);
        assertThat(result).hasSize(1);
        verify(recordRepository).findByTypeAndDateBetween(RecordType.INCOME, from, to);
        verify(recordRepository, never()).findByDateBetween(any(), any());
    }

    @Test
    void filterRecords_typeAndCategoryAndDateRange_usesFullCombinedQuery() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 3, 31);
        when(recordRepository.findByTypeAndCategoryAndDateBetween(RecordType.INCOME, "Salary", from, to))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(RecordType.INCOME, "Salary", from, to);
        assertThat(result).hasSize(1);
        verify(recordRepository).findByTypeAndCategoryAndDateBetween(RecordType.INCOME, "Salary", from, to);
    }

    @Test
    void filterRecords_noParams_returnsAll() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findAll()).thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(null, null, null, null);
        assertThat(result).hasSize(1);
        verify(recordRepository).findAll();
    }

    @Test
    void filterRecords_viewer_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);

        assertThatThrownBy(() -> recordService.filterRecords(null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
