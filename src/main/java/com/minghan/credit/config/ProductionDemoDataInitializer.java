package com.minghan.credit.config;

import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.Role;
import com.minghan.credit.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "demo.data.enabled",
        havingValue = "true")
public class ProductionDemoDataInitializer implements CommandLineRunner {
    private static final String RM_USERNAME = "demo_rm";
    private static final String REVIEWER_USERNAME = "demo_reviewer";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String rmPassword;
    private final String reviewerPassword;

    public ProductionDemoDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${demo.rm.password}") String rmPassword,
            @Value("${demo.reviewer.password}") String reviewerPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.rmPassword = rmPassword;
        this.reviewerPassword = reviewerPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        validatePassword("demo.rm.password", rmPassword);
        validatePassword("demo.reviewer.password", reviewerPassword);

        createUserIfMissing(RM_USERNAME, rmPassword, Role.RM);
        createUserIfMissing(REVIEWER_USERNAME, reviewerPassword, Role.REVIEWER);
    }

    private void validatePassword(String propertyName, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    propertyName + " must be configured when demo data is enabled");
        }
    }

    private void createUserIfMissing(String username, String password, Role role) {
        if (appUserRepository.findByUsername(username).isEmpty()) {
            appUserRepository.save(
                    new AppUser(username, passwordEncoder.encode(password), role));
        }
    }
}
