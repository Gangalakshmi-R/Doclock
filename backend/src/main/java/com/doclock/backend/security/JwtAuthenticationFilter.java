package com.doclock.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        // =====================================================
        // TEMPORARY DEBUG
        // =====================================================

        System.out.println(
                "JWT REQUEST: "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
                        + " | AUTH HEADER PRESENT: "
                        + (authorization != null)
        );


        // =====================================================
        // NO TOKEN
        // =====================================================

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // =====================================================
        // EXTRACT TOKEN
        // =====================================================

        String token =
                authorization.substring(7);


        try {

            // =================================================
            // VALIDATE TOKEN
            // =================================================

            boolean valid =
                    jwtService.isValid(token);


            System.out.println(
                    "JWT VALID: "
                            + valid
            );


            // =================================================
            // AUTHENTICATE USER
            // =================================================

            if (valid) {

                String username =
                        jwtService.extractUsername(
                                token
                        );


                System.out.println(
                        "JWT USERNAME: "
                                + username
                );


                UsernamePasswordAuthenticationToken
                        authentication =

                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );


                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }

        } catch (Exception e) {

            // =================================================
            // JWT ERROR
            // =================================================

            System.out.println(
                    "JWT ERROR: "
                            + e.getMessage()
            );

            e.printStackTrace();

            SecurityContextHolder
                    .clearContext();
        }


        // =====================================================
        // CONTINUE FILTER CHAIN
        // =====================================================

        filterChain.doFilter(
                request,
                response
        );
    }
}