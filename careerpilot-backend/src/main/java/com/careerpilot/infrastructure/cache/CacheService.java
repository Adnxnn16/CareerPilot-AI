package com.careerpilot.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheMatch(UUID userId, UUID jobId, Object matchResult) {
        String key = "match:%s:%s".formatted(userId, jobId);
        redisTemplate.opsForValue().set(key, matchResult, Duration.ofHours(24));

        // Index key so eviction never needs a blocking KEYS scan
        String indexKey = "match-keys:" + userId;
        redisTemplate.opsForSet().add(indexKey, key);
        redisTemplate.expire(indexKey, Duration.ofHours(24));
    }

    public Object getCachedMatch(UUID userId, UUID jobId) {
        return redisTemplate.opsForValue().get("match:%s:%s".formatted(userId, jobId));
    }

    public void evictAllMatchesForUser(UUID userId) {
        String indexKey = "match-keys:" + userId;
        Set<Object> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys.stream().map(Object::toString).toList());
        }
        redisTemplate.delete(indexKey);
    }
}
