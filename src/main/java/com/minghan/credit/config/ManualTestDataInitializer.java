package com.minghan.credit.config;

import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.Role;
import com.minghan.credit.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-test")
// 測試帳號只由 manual-test profile 建立，不寫入 Flyway schema history。
public class ManualTestDataInitializer implements CommandLineRunner {
    private static final String RM_USERNAME = "stage5_rm";
    private static final String RM_PASSWORD = "RmPass123!";
    private static final String REVIEWER_USERNAME = "stage5_reviewer";
    private static final String REVIEWER_PASSWORD = "ReviewerPass123!";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public ManualTestDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing(RM_USERNAME, RM_PASSWORD, Role.RM);
        createUserIfMissing(REVIEWER_USERNAME, REVIEWER_PASSWORD, Role.REVIEWER);
    }

    private void createUserIfMissing(String username, String password, Role role) {
        // 啟動可重複執行；僅新帳號需要即時產生 BCrypt 雜湊。
        if (appUserRepository.findByUsername(username).isEmpty()) {
            appUserRepository.save(
                    new AppUser(username, passwordEncoder.encode(password), role));
        }
    }
}
