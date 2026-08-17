package com.doclock.backend.controller;

import com.doclock.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final JwtService jwtService;


    @Value("${doclock.auth.username}")
    private String configuredUsername;


    @Value("${doclock.auth.password}")
    private String configuredPassword;


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request) {


        String username =
                request.get("username");


        String password =
                request.get("password");


        // =================================================
        // CHECK CREDENTIALS
        // =================================================

        if (
                !configuredUsername.equals(username)
                ||
                !configuredPassword.equals(password)
        ) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Invalid username or password"
                            )
                    );
        }


        // =================================================
        // GENERATE JWT
        // =================================================

        String token =
                jwtService.generateToken(
                        username
                );


        // =================================================
        // RESPONSE
        // =================================================

        return ResponseEntity.ok(

                Map.of(
                        "token",
                        token,

                        "username",
                        username
                )

        );
    }
}