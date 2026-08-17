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

        String username = request.get("username");
        String password = request.get("password");


        // =================================================
        // DEBUG INFORMATION
        // =================================================

        System.out.println(
                "LOGIN REQUEST"
        );

        System.out.println(
                "Received username: "
                        + username
        );

        System.out.println(
                "Configured username: "
                        + configuredUsername
        );

        System.out.println(
                "Username matches: "
                        + configuredUsername.equals(username)
        );

        System.out.println(
                "Password received: "
                        + (password != null)
        );

        System.out.println(
                "Configured password exists: "
                        + (configuredPassword != null
                        && !configuredPassword.isBlank())
        );


        // =================================================
        // VALIDATE INPUT
        // =================================================

        if (username == null || password == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "Username and password are required"
                            )
                    );
        }


        // =================================================
        // CHECK CREDENTIALS
        // =================================================

        boolean usernameMatches =
                configuredUsername.equals(username);

        boolean passwordMatches =
                configuredPassword.equals(password);


        System.out.println(
                "Password matches: "
                        + passwordMatches
        );


        if (!usernameMatches || !passwordMatches) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
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
                jwtService.generateToken(username);


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