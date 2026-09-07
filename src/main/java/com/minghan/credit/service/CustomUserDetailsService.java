package com.minghan.credit.service;

import com.minghan.credit.entity.AppUser;
import com.minghan.credit.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
// 將應用程式的 AppUser 轉成 Spring Security 可用的 UserDetails。
public class CustomUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        // hasRole("RM") 會檢查 ROLE_RM，因此統一在 authority 前加上 ROLE_。
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities("ROLE_" + appUser.getRole().name())
                .build();
    }
}
