package com.examensw1.umlcollab.features.auth.service;

import com.examensw1.umlcollab.features.auth.dto.AuthResponse;
import com.examensw1.umlcollab.features.auth.dto.LoginRequest;
import com.examensw1.umlcollab.features.auth.dto.RegisterRequest;
import com.examensw1.umlcollab.features.auth.model.AppUser;
import com.examensw1.umlcollab.features.auth.repository.AppUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwords;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }

        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        return response(users.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }
        return response(user);
    }

    public String subject(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    private AuthResponse response(AppUser user) {
        String token = Jwts.builder().subject(user.getEmail()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMinutes * 60_000))
                .signWith(key()).compact();
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
