package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.FinancialRecordRequest;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private FinancialRecordRequest sampleRequest;

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
        sampleRecord.setDate(LocalDateTime.of(2025, 1, 15, 0, 0));
        sampleRecord.setDeleted(false);

        sampleRequest = new FinancialRecordRequest();
        sampleRequest.setAmount(5000.0);
        sampleRequest.setType(RecordType.INCOME);
        sampleRequest.setCategory("Salary");
        sampleRequest.setDate(LocalDateTime.of(2025, 1, 15, 0, 0));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // --- createRecord ---

    @Test
    void createRecord_viewerCannotCreate_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.createRecord(sampleRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createRecord_analystCannotCreate_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        assertThatThrownBy(() -> recordService.createRecord(sampleRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createRecord_adminCanCreate_savesRecord() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.save(any())).thenReturn(sampleRecord);

        FinancialRecord result = recordService.createRecord(sampleRequest);

        assertThat(result.getId()).isEqualTo("rec-1");
        verify(recordRepository).save(any(FinancialRecord.class));
    }

    // --- getAllRecords ---

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

    // --- updateRecord ---

    @Test
    void updateRecord_admin_shouldSucceed() {
        when(userService.resolveCaller()).thenReturn(adminUser);
        when(recordRepository.findById("rec-1")).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any())).thenReturn(sampleRecord);

        FinancialRecordRequest req = new FinancialRecordRequest();
        req.setAmount(500.0);
        req.setType(RecordType.EXPENSE);
        req.setCategory("Food");
        req.setDate(LocalDateTime.now());

        FinancialRecord result = recordService.updateRecord("rec-1", req);
        assertThat(result.getAmount()).isEqualTo(500.0);
    }

    @Test
    void updateRecord_analyst_shouldThrow() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        assertThatThrownBy(() -> recordService.updateRecord("rec-1", sampleRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateRecord_viewer_shouldThrow() {
        when(userService.resolveCaller()).thenReturn(viewerUser);

        assertThatThrownBy(() -> recordService.updateRecord("rec-1", sampleRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- deleteRecord ---

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
        // should NOT call deleteById — it's a soft delete
        verify(recordRepository, never()).deleteById(any());
    }

    // --- filterRecords ---

    @Test
    void filterRecords_viewer_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.filterRecords(null, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void filterRecords_typeAndDateRange_usesDynamicQuery() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        LocalDateTime fromDT = from.atStartOfDay();
        LocalDateTime toDT = to.atTime(23, 59, 59);

        when(recordRepository.filterDynamic(
                RecordType.INCOME, null, fromDT, toDT, null, null))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result =
                recordService.filterRecords(RecordType.INCOME, null, from, to, null);

        assertThat(result).hasSize(1);
        verify(recordRepository).filterDynamic(RecordType.INCOME, null, fromDT, toDT, null, null);
    }

    @Test
    void filterRecords_keywordSearch_usesDynamicQuery() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        when(recordRepository.filterDynamic(null, null, null, null, "sal", null))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result =
                recordService.filterRecords(null, null, null, null, "sal");

        assertThat(result).hasSize(1);
        verify(recordRepository).filterDynamic(null, null, null, null, "sal", null);
    }

    @Test
    void filterRecords_noParams_returnsAll() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        when(recordRepository.filterDynamic(null, null, null, null, null, null))
                .thenReturn(List.of(sampleRecord));

        List<FinancialRecord> result = recordService.filterRecords(null, null, null, null, null);
        assertThat(result).hasSize(1);
        verify(recordRepository).filterDynamic(null, null, null, null, null, null);
    }

    @Test
    void filterRecords_onlyFrom_shouldThrow() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        assertThatThrownBy(() ->
            recordService.filterRecords(null, null, LocalDate.now(), null, null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("'from' and 'to' must be provided together");
    }

    @Test
    void filterRecords_fromAfterTo_shouldThrow() {
        when(userService.resolveCaller()).thenReturn(analystUser);

        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() ->
            recordService.filterRecords(null, null, from, to, null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("'from' date must not be after 'to' date");
    }

    // --- getPaginated ---

    @Test
    void getPaginated_viewer_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.getPaginated(0, 10))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- getRecordById ---

    @Test
    void getRecordById_viewer_throwsAccessDenied() {
        when(userService.resolveCaller()).thenReturn(viewerUser);
        assertThatThrownBy(() -> recordService.getRecordById("rec-1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getRecordById_analyst_returnsRecord() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findById("rec-1")).thenReturn(Optional.of(sampleRecord));

        FinancialRecord result = recordService.getRecordById("rec-1");
        assertThat(result.getId()).isEqualTo("rec-1");
    }

    @Test
    void getRecordById_notFound_throwsResourceNotFound() {
        when(userService.resolveCaller()).thenReturn(analystUser);
        when(recordRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.getRecordById("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}