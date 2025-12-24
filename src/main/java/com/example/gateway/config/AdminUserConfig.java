package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class AdminUserConfig {

  @Value("${admin.usernameEnv:ADMIN_USERNAME}")
  private String usernameEnv;

  @Value("${admin.passwordEnv:ADMIN_PASSWORD}")
  private String passwordEnv;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    String username = getenvOrThrow(usernameEnv);
    String password = getenvOrThrow(passwordEnv);

    UserDetails admin = User.withUsername(username)
        .password(encoder.encode(password))
        .roles("ADMIN")
        .build();

    return new InMemoryUserDetailsManager(admin);
  }

  private static String getenvOrThrow(String key) {
    String v = System.getenv(key);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("Missing required environment variable: " + key);
    }
    return v;
  }
}
