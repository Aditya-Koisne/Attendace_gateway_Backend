package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain security(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    // ✅ allow browser preflight
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // ✅ device endpoints (open)
                    .requestMatchers("/iclock/**").permitAll()
                    .requestMatchers("/healthz", "/healthz/**").permitAll()

                    // ✅ keep health open
                    .requestMatchers("/actuator/health").permitAll()

                    // ✅ admin APIs + full actuator require ADMIN
                    .requestMatchers("/api/admin/**", "/actuator/**").hasRole("ADMIN")

                    // (optional) if you want "/" not to be 401 (useful while nginx serves frontend)
                     .requestMatchers("/").permitAll()

                    .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

    return http.build();
  }
}
