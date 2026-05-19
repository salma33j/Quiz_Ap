
package com.exemple.quiz_app.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // ✅ Nouvelle clé spécifique pour QuizApp
    private static final byte[] SECRET_BYTES = new byte[]{
            81, 117, 105, 122, 65, 112, 112, 83, 101, 99, 114, 101, 116, 75,
            101, 121, 50, 48, 50, 53, 81, 117, 105, 122, 65, 112, 112, 83,
            117, 112, 101, 114, 83, 101, 99, 117, 114, 101, 75, 101, 121, 70,
            111, 114, 83, 112, 114, 105, 110, 103, 66, 111, 111, 116, 74, 87,
            84, 50, 53, 54, 66, 121, 116, 101, 115, 51, 50, 65, 108, 103, 111,
            114, 105, 116, 104, 109, 72, 83, 53, 49, 50, 83, 101, 99, 117, 114,
            101, 75, 101, 121
    };

    private final Key jwtSecret = Keys.hmacShaKeyFor(SECRET_BYTES);
    private final int jwtExpirationMs = 86400000; // 24 heures

    // ✅ Générer token avec email et rôle
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }

    // ✅ Obtenir tous les claims
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ Extraire l'email du token
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // ✅ Extraire le rôle du token
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ✅ Valider le token
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
