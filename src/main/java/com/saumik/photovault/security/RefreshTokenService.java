package com.saumik.photovault.security;

import com.saumik.photovault.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String RT_PREFIX = "rt:";
    private static final String USER_PREFIX = "rt:user:";
    private final StringRedisTemplate redisTemplate;

    @Value("${refresh.token-expiration}")
    private long refreshTtl;

    public String createRefreshToken(UUID userId) {

        cleanExpiredTokensLazily(userId);

        String token = UUID.randomUUID().toString();
        String tokenKey = RT_PREFIX + token;
        String userKey = USER_PREFIX + userId;
        Duration ttl = Duration.ofMillis(refreshTtl);

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                operations.opsForValue().set(tokenKey, userId.toString(), ttl);
                operations.opsForSet().add(userKey, token);

                operations.expire(userKey, ttl.plus(Duration.ofDays(1)));

                return operations.exec();
            }
        });

        return token;

    }

    public UUID getUserId(String refreshToken) {

        String userId = redisTemplate.opsForValue()
                .get(RT_PREFIX + refreshToken);

        if (userId == null) {
            throw new InvalidRefreshTokenException("Refresh token expired or invalid.");
        }

        return UUID.fromString(userId);
    }

    public void revokeRefreshToken(UUID id,String token) {

        String tokenKey = RT_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(tokenKey);

        if(userId != null){

            if(!UUID.fromString(userId).equals(id)) {
                throw new InvalidRefreshTokenException(
                        "Refresh token does not belong to the authenticated user."
                );
            }

            String userKey = USER_PREFIX + userId;
            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.delete(tokenKey);
                    operations.opsForSet().remove(userKey, token);
                    return operations.exec();
                }
            });
        }
    }

    public void revokeAllRefreshTokens(UUID userId) {

        String userKey = USER_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);

        if (tokens != null && !tokens.isEmpty()) {
            Set<String> keysToDelete = tokens.stream()
                    .map(t -> RT_PREFIX + t)
                    .collect(Collectors.toSet());

            keysToDelete.add(userKey);
            redisTemplate.delete(keysToDelete);
        }
    }

    private void cleanExpiredTokensLazily(UUID userId) {
        String userKey = USER_PREFIX + userId;
        Set<String> trackedTokens = redisTemplate.opsForSet().members(userKey);

        if (trackedTokens != null && !trackedTokens.isEmpty()) {
            Set<String> expiredTokens = trackedTokens.stream()
                    .filter(token -> Boolean.FALSE.equals(redisTemplate.hasKey(RT_PREFIX + token)))
                    .collect(Collectors.toSet());

            if (!expiredTokens.isEmpty()) {
                redisTemplate.opsForSet().remove(userKey, expiredTokens.toArray());
            }
        }
    }
}