package com.saumik.photovault.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final StringRedisTemplate redisTemplate;
    private final String TOKEN_PREFIX = "ev:token:";
    private final String USER_PREFIX = "ev:user:";
    private final long ttl = 60;

    @Transactional
    public String createToken(UUID userId){

        String existToken = getToken(userId);
        if(existToken != null){
            deleteToken(existToken);
        }


        String token = UUID.randomUUID().toString();
        String keyToken = TOKEN_PREFIX + token;
        String keyUser = USER_PREFIX + userId.toString();
        redisTemplate.opsForValue().set(keyToken,userId.toString(), Duration.ofMinutes(ttl));
        redisTemplate.opsForValue().set(keyUser,token, Duration.ofMinutes(ttl));
        return token;
    }

    public void deleteToken(String token){
        UUID userId = getId(token);
        if(userId != null){
            redisTemplate.delete(TOKEN_PREFIX + token);
            redisTemplate.delete(USER_PREFIX + userId);
        }
    }


    public UUID getId(String token){
        String key = TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if(Objects.isNull(value)){
            return null;
        }
        return UUID.fromString(value);
    }

    public String getToken(UUID userId){
        String key = USER_PREFIX + userId.toString();
        return redisTemplate.opsForValue().get(key);
    }

}