package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.UserUpdateRequest;
import com.zorvyn.finance.entity.Role;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.AccessDeniedException;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User viewerUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId("admin-1");
        adminUser.setName("Admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);

        viewerUser = new User();
        viewerUser.setId("viewer-1");
        viewerUser.setName("Viewer");
        viewerUser.setEmail("viewer@test.com");
        viewerUser.setRole(Role.VIEWER);
        viewerUser.setActive(true);

        inactiveUser = new User();
        inactiveUser.setId("inactive-1");
        inactiveUser.setName("Inactive");
        inactiveUser.setEmail("inactive@test.com");
        inactiveUser.setRole(Role.ANALYST);
        inactiveUser.setActive(false);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // resolveCaller tests section

    @Test
    void resolveCaller_missingHeader_throwsUnauthorized() {
        AuthContext.set(null);
        assertThatThrownBy(() -> userService.resolveCaller())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing or invalid authentication token");
    }
    @Test
    void resolveCaller_inactiveUser_throwsUnauthorized() {
        AuthContext.set("inactive-1");
        when(userRepository.findById("inactive-1")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> userService.resolveCaller())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void resolveCaller_activeUser_returnsUser() {
        AuthContext.set("admin-1");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));

        User result = userService.resolveCaller();
        assertThat(result.getId()).isEqualTo("admin-1");
    }

    // getAllUsers access control section

    @Test
    void getAllUsers_callerIsViewer_throwsAccessDenied() {
        AuthContext.set("viewer-1");
        when(userRepository.findById("viewer-1")).thenReturn(Optional.of(viewerUser));

        assertThatThrownBy(() -> userService.getAllUsers())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void getAllUsers_callerIsAdmin_returnsList() {
        AuthContext.set("admin-1");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(java.util.List.of(adminUser, viewerUser));

        var result = userService.getAllUsers();
        assertThat(result).hasSize(2);
    }


    @Test
    void updateUser_adminCanChangeRole() {
        AuthContext.set("admin-1");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("viewer-1")).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole(Role.ANALYST);

        User updated = userService.updateUser("viewer-1", req);
        assertThat(updated.getRole()).isEqualTo(Role.ANALYST);
    }

    @Test
    void updateUser_adminCanDeactivateUser() {
        AuthContext.set("admin-1");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("viewer-1")).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setActive(false);

        User updated = userService.updateUser("viewer-1", req);
        assertThat(updated.isActive()).isFalse();
    }


    @Test
    void createUser_duplicateEmail_throwsIllegalArgument() {
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(adminUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    void createUser_newEmail_savesAndReturns() {
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(userRepository.save(adminUser)).thenReturn(adminUser);

        User saved = userService.createUser(adminUser);
        assertThat(saved.getEmail()).isEqualTo("admin@test.com");
        verify(userRepository, times(1)).save(adminUser);
    }
}
