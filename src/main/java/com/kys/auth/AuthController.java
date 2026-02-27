package com.kys.auth;

import com.kys.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import com.kys.auth.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            String access = jwtUtil.generateAccessToken(request.username());
            String refresh = jwtUtil.generateRefreshToken(request.username());
            String role = userRepo.findByUsername(request.username())
                .map(u -> u.getRole().name()).orElse("USER");
            return ResponseEntity.ok(Map.of(
                "access", access, "refresh", refresh,
                "username", request.username(), "role", role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Kullanıcı adı veya şifre hatalı"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh");
        if (refreshToken != null && jwtUtil.isValid(refreshToken)) {
            String username = jwtUtil.extractUsername(refreshToken);
            String newAccess = jwtUtil.generateAccessToken(username);
            return ResponseEntity.ok(Map.of("access", newAccess));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Geçersiz token"));
    }

    record LoginRequest(String username, String password) {}
}
