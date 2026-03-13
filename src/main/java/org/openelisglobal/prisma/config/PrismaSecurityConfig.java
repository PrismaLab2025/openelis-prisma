package org.openelisglobal.prisma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1)
public class PrismaSecurityConfig {

    @Bean
    public SecurityFilterChain prismaSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/rest/prisma/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll());

        return http.build();
    }
}
