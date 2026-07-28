package com.barbearia.saas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Serviço responsável por gerar, validar e extrair claims de tokens JWT. */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Decoders.BASE64.decode(
                    java.util.Base64.getEncoder().encodeToString(keyBytes));
        }
        this.secretKey = Keys.hmacShaKeyFor(padKey(keyBytes));
        this.expirationMs = expirationMs;
    }

    private byte[] padKey(byte[] keyBytes) {
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
        return padded;
    }

    /** Gera um token JWT para o usuário informado. */
    public String generateToken(UsuarioPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", principal.getId());
        claims.put("bid", principal.getBarbeariaId());
        claims.put("role", principal.getRole().name());
        claims.put("nome", principal.getNome());
        if (principal.getClienteId() != null) {
            claims.put("cid", principal.getClienteId());
        }
        if (principal.getBarbeiroId() != null) {
            claims.put("brid", principal.getBarbeiroId());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(principal.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /** Extrai o e-mail (username) contido no token JWT. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Indica se o token JWT é válido para o usuário informado. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
