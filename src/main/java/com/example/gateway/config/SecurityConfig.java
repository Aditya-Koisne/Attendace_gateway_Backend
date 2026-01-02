package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            // ❌ disable CSRF (devices + curl + admin APIs)
            .csrf(csrf -> csrf.disable())

            // ✅ authorization rules
            .authorizeHttpRequests(auth -> auth
                    // 🔓 device + ingest paths (DO NOT TOUCH)
                    .requestMatchers(
                            "/iclock/**",
                            "/actuator/**",
                            "/healthz"
                    ).permitAll()

                    // 🔒 admin APIs
                    .requestMatchers("/api/admin/**").authenticated()

                    // default: allow others
                    .anyRequest().permitAll()
            )

            // ✅ basic auth only
            .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public UserDetailsService users() {
    UserDetails admin = User
            .withUsername("admin")
            .password("admin123")
            .roles("ADMIN")
            .build();

    return new InMemoryUserDetailsManager(admin);
  }

  // ⚠️ for simplicity only (OK for your use case)
  @Bean
  public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
  }
}
