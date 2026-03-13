package org.openelisglobal.prisma.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security config for Prisma Integration Layer only.
 * @Order(1) ensures this runs BEFORE the main OpenELIS SecurityConfig.
 * Only applies to /rest/prisma/** — everything else is untouched.
 */
@Configuration
@Order(1)
public class PrismaSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${prisma.api.key:prisma-secret-key}")
    private String apiKey;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // Apply ONLY to /rest/prisma/** — nothing else
            .requestMatcher(new AntPathRequestMatcher("/rest/prisma/**"))

            // No CSRF needed — we use API key authentication
            .csrf().disable()

            // Stateless — no session created or used
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            // Allow all requests to /rest/prisma/**
            // (API key validation is done inside the controller)
            .authorizeRequests()
                .antMatchers("/rest/prisma/**").permitAll();
    }
}
