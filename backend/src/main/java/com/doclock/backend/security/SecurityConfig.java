package com.doclock.backend.security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {


    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;


    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {


        http


                // =========================================
                // CSRF
                // =========================================

                .csrf(
                        csrf ->
                                csrf.disable()
                )


                // =========================================
                // CORS
                // =========================================

                .cors(
                        cors ->
                                cors.configurationSource(
                                        corsConfigurationSource()
                                )
                )


                // =========================================
                // STATELESS JWT
                // =========================================

                .sessionManagement(

                        session ->

                                session.sessionCreationPolicy(

                                        SessionCreationPolicy
                                                .STATELESS

                                )
                )


                // =========================================
                // AUTHORIZATION
                // =========================================

                .authorizeHttpRequests(

                        auth -> auth


                                // LOGIN
                                .requestMatchers(
                                        "/api/auth/login"
                                )
                                .permitAll()


                                // SWAGGER
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()


                                // EVERYTHING ELSE
                                .anyRequest()
                                .authenticated()

                )


                // =========================================
                // JWT FILTER
                // =========================================

                .addFilterBefore(

                        jwtAuthenticationFilter,

                        UsernamePasswordAuthenticationFilter
                                .class

                );


        return http.build();
    }


    // =====================================================
    // CORS CONFIGURATION
    // =====================================================

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {


        CorsConfiguration configuration =
                new CorsConfiguration();


        configuration.setAllowedOrigins(

                List.of(

                        "http://localhost:5173",

                        "http://localhost:3000"

                )

        );


        configuration.setAllowedMethods(

                List.of(

                        "GET",

                        "POST",

                        "PUT",

                        "DELETE",

                        "OPTIONS"

                )

        );


        configuration.setAllowedHeaders(

                List.of("*")

        );


        configuration.setAllowCredentials(
                true
        );


        UrlBasedCorsConfigurationSource
                source =

                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(

                "/**",

                configuration

        );


        return source;
    }
}