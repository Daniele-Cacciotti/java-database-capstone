package com.project.back_end.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenService {

    private static final Set<String> ROLES = Set.of("admin", "doctor", "patient");
    private static final String ROLE_CLAIM = "role";

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SecretKey signingKey;
    private final JwtParser parser;

    public TokenService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            @Value("${jwt.secret}") String secret) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.signingKey = getSigningKey(secret);
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String generateToken(String identifier, String role) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("A token identifier is required.");
        }
        if (role == null || !ROLES.contains(role)) {
            throw new IllegalArgumentException("Unknown token role.");
        }

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(identifier)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(7, ChronoUnit.DAYS)))
                .signWith(signingKey)
                .compact();
    }

    public String extractIdentifier(String token) {
        return verifiedClaims(token).getSubject();
    }

    public boolean validateToken(String token, String role) {
        if (token == null || token.isBlank() || role == null || !ROLES.contains(role)) {
            return false;
        }

        try {
            Claims claims = verifiedClaims(token);
            if (!role.equals(claims.get(ROLE_CLAIM))) {
                return false;
            }

            String identifier = claims.getSubject();
            return switch (role) {
                case "admin" -> adminRepository.findByUsername(identifier) != null;
                case "doctor" -> doctorRepository.findByEmail(identifier) != null;
                case "patient" -> patientRepository.findByEmail(identifier) != null;
                default -> false;
            };
        } catch (JwtException | IllegalArgumentException | DataAccessException e) {
            return false;
        }
    }

    private Claims verifiedClaims(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        String identifier = claims.getSubject();
        if (identifier == null || identifier.isBlank() || claims.getExpiration() == null) {
            throw new MalformedJwtException("Token must contain a subject and expiration.");
        }
        return claims;
    }

    private static SecretKey getSigningKey(String secret) {
        if (secret == null || !secret.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("jwt.secret must contain 64 hexadecimal characters.");
        }
        return Keys.hmacShaKeyFor(HexFormat.of().parseHex(secret));
    }
}
