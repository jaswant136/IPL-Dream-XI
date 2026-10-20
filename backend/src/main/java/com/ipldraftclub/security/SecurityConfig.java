package com.ipldraftclub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipldraftclub.config.AppProperties;
import com.ipldraftclub.dto.ErrorResponse;
import com.ipldraftclub.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Production security configuration. Stateless, token-based, with an explicit public allow-list.
 * Test-only authentication lives in src/test and is never part of this class.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenVerifier verifier,
                                                   UserService userService,
                                                   AppProperties props,
                                                   ObjectMapper objectMapper) throws Exception {
        FirebaseAuthenticationFilter firebaseFilter =
                new FirebaseAuthenticationFilter(verifier, userService, props.security().requireEmailVerified());

        http
                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API, no cookies/sessions
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/health",
                                "/api/v1/teams",
                                "/api/v1/teams/*/seasons",
                                "/api/v1/teams/*/seasons/*/players",
                                "/api/v1/leaderboard",
                                "/api/v1/game-modes",
                                "/api/v1/shared/*").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint(objectMapper))
                        .accessDeniedHandler(accessDenied(objectMapper)))
                .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.cors().allowedOrigins() == null ? List.of() : props.cors().allowedOrigins();
        cfg.setAllowedOrigins(origins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "Accept"));
        // Bearer tokens travel in a header, so cookies/credentials are not needed (and never combined with wildcards).
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    private AuthenticationEntryPoint entryPoint(ObjectMapper mapper) {
        return (request, response, ex) -> {
            Object reason = request.getAttribute(FirebaseAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
            String message = reason != null ? reason.toString() : "Authentication is required to access this resource.";
            write(response, mapper, HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
        };
    }

    private AccessDeniedHandler accessDenied(ObjectMapper mapper) {
        return (request, response, ex) -> write(response, mapper, HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.", request.getRequestURI());
    }

    private void write(HttpServletResponse response, ObjectMapper mapper, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(status, message, path, List.of()));
    }
}
