package com.doclock.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.util.Date;


@Service
public class JwtService {


    private final SecretKey secretKey;

    private final long expiration;


    public JwtService(

            @Value("${doclock.jwt.secret}")
            String secret,

            @Value("${doclock.jwt.expiration}")
            long expiration

    ) {


        this.secretKey =
                Keys.hmacShaKeyFor(

                        secret.getBytes(
                                StandardCharsets.UTF_8
                        )

                );


        this.expiration =
                expiration;
    }


    // =====================================================
    // GENERATE TOKEN
    // =====================================================

    public String generateToken(
            String username
    ) {


        Date now =
                new Date();


        Date expiry =
                new Date(
                        now.getTime()
                        +
                        expiration
                );


        return Jwts.builder()

                .subject(
                        username
                )

                .issuedAt(
                        now
                )

                .expiration(
                        expiry
                )

                .signWith(
                        secretKey
                )

                .compact();
    }


    // =====================================================
    // EXTRACT USERNAME
    // =====================================================

    public String extractUsername(
            String token
    ) {


        return getClaims(
                token
        ).getSubject();
    }


    // =====================================================
    // VALIDATE TOKEN
    // =====================================================

    public boolean isValid(
            String token
    ) {


        try {

            Claims claims =
                    getClaims(
                            token
                    );


            return claims
                    .getExpiration()
                    .after(
                            new Date()
                    );


        } catch (Exception e) {

            return false;
        }
    }


    // =====================================================
    // GET CLAIMS
    // =====================================================

    private Claims getClaims(
            String token
    ) {


        return Jwts.parser()

                .verifyWith(
                        secretKey
                )

                .build()

                .parseSignedClaims(
                        token
                )

                .getPayload();
    }
}