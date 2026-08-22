package com.saumik.photovault.security;


import com.saumik.photovault.entity.User;
import com.saumik.photovault.exception.InvalidTokenException;
import com.saumik.photovault.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    // generate access token
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                //.claim("ver",user.getPasswordVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSecretKey())
                .compact();
    }

    // get user id from token
    public UUID extractUserId(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    // get password version from token
    public Integer extractPasswordVersion(String token) {
        Claims claims = getClaimsFromToken(token);
        return Integer.parseInt(claims.get("ver").toString());
    }

    // valid?
    public boolean isTokenValid(String token, AppUserDetails user) {
        Claims claims = getClaimsFromToken(token);
        UUID id = UUID.fromString(claims.getSubject());
        Integer version = claims.get("ver", Integer.class);
        return id.equals(user.getId());
    }

    // utils
    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getClaimsFromToken(String token) {
        try{
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("JWT expired");
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token");
        }
    }
}
