package com.minghan.credit.config;

import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.Role;
import com.minghan.credit.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionDemoDataInitializerTest {
    private static final String RM_PASSWORD = "rm-password-from-environment";
    private static final String REVIEWER_PASSWORD = "reviewer-password-from-environment";

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsMissingDemoUsersWithEncodedPasswordsAndExpectedRoles() {
        when(appUserRepository.findByUsername("demo_rm"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("demo_reviewer"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(RM_PASSWORD)).thenReturn("encoded-rm-password");
        when(passwordEncoder.encode(REVIEWER_PASSWORD))
                .thenReturn("encoded-reviewer-password");

        initializer(RM_PASSWORD, REVIEWER_PASSWORD).run();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(2)).save(captor.capture());

        List<AppUser> savedUsers = captor.getAllValues();
        assertUser(savedUsers.get(0), "demo_rm", "encoded-rm-password", Role.RM);
        assertUser(
                savedUsers.get(1),
                "demo_reviewer",
                "encoded-reviewer-password",
                Role.REVIEWER);
        verify(passwordEncoder).encode(RM_PASSWORD);
        verify(passwordEncoder).encode(REVIEWER_PASSWORD);
    }

    @Test
    void skipsExistingDemoUsersWithoutUpdatingThem() {
        AppUser existingRm = new AppUser("demo_rm", "existing-rm-password", Role.RM);
        AppUser existingReviewer = new AppUser(
                "demo_reviewer",
                "existing-reviewer-password",
                Role.REVIEWER);
        when(appUserRepository.findByUsername("demo_rm"))
                .thenReturn(Optional.of(existingRm));
        when(appUserRepository.findByUsername("demo_reviewer"))
                .thenReturn(Optional.of(existingReviewer));

        initializer(RM_PASSWORD, REVIEWER_PASSWORD).run();

        verify(appUserRepository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder);
        assertUser(existingRm, "demo_rm", "existing-rm-password", Role.RM);
        assertUser(
                existingReviewer,
                "demo_reviewer",
                "existing-reviewer-password",
                Role.REVIEWER);
    }

    @Test
    void rejectsBlankRmPasswordBeforeWritingUsers() {
        ProductionDemoDataInitializer initializer = initializer(
                "   ",
                REVIEWER_PASSWORD);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                initializer::run);

        assertEquals(
                "demo.rm.password must be configured when demo data is enabled",
                exception.getMessage());
        verifyNoInteractions(appUserRepository, passwordEncoder);
    }

    @Test
    void rejectsNullReviewerPasswordBeforeWritingUsers() {
        ProductionDemoDataInitializer initializer = initializer(RM_PASSWORD, null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                initializer::run);

        assertEquals(
                "demo.reviewer.password must be configured when demo data is enabled",
                exception.getMessage());
        verifyNoInteractions(appUserRepository, passwordEncoder);
    }

    @Test
    void initializesUsersWithinATransaction() throws NoSuchMethodException {
        Transactional transactional = ProductionDemoDataInitializer.class
                .getMethod("run", String[].class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    private ProductionDemoDataInitializer initializer(
            String rmPassword,
            String reviewerPassword) {
        return new ProductionDemoDataInitializer(
                appUserRepository,
                passwordEncoder,
                rmPassword,
                reviewerPassword);
    }

    private void assertUser(
            AppUser user,
            String username,
            String password,
            Role role) {
        assertEquals(username, user.getUsername());
        assertEquals(password, user.getPassword());
        assertEquals(role, user.getRole());
    }
}
