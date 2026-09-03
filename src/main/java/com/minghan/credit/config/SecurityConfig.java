package com.minghan.credit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Stage 2 開發期間暫時放行 Health 與 Company API，正式驗證與權限控管留待後續 Stage
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/companies", "/api/companies/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }
}
