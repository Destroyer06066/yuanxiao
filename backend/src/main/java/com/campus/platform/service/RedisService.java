package com.campus.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis 服务封装：
 * 1. Session 管理（JWT jti 存储）
 * 2. 短信验证码存储
 * 3. 分布式锁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String SESSION_PREFIX   = "session:";
    private static final String SMS_CODE_PREFIX   = "sms_code:";
    private static final String SMS_LIMIT_PREFIX  = "sms_limit:";
    private static final String LOCK_PREFIX       = "lock:";

    private static final Duration SESSION_TTL     = Duration.ofHours(8);
    private static final Duration SMS_CODE_TTL    = Duration.ofMinutes(5);
    private static final Duration SMS_LIMIT_TTL   = Duration.ofDays(1);

    // ========== Session ==========

    public void createSession(UUID accountId, String jti) {
        redisTemplate.opsForValue().set(SESSION_PREFIX + accountId, jti, SESSION_TTL);
        log.debug("创建会话: accountId={}, jti={}", accountId, jti);
    }

    public String getSession(UUID accountId) {
        return redisTemplate.opsForValue().get(SESSION_PREFIX + accountId);
    }

    public void deleteSession(UUID accountId) {
        redisTemplate.delete(SESSION_PREFIX + accountId);
        log.debug("删除会话: accountId={}", accountId);
    }

    public void refreshSession(UUID accountId) {
        redisTemplate.expire(SESSION_PREFIX + accountId, SESSION_TTL);
    }

    // ========== 短信验证码 ==========

    public void saveSmsCode(String username, String code) {
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + username, code, SMS_CODE_TTL);
    }

    public String getSmsCode(String username) {
        return redisTemplate.opsForValue().get(SMS_CODE_PREFIX + username);
    }

    public void deleteSmsCode(String username) {
        redisTemplate.delete(SMS_CODE_PREFIX + username);
    }

    public boolean isSmsRateLimited(String phone) {
        String key = SMS_LIMIT_PREFIX + phone;
        String count = redisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count) >= 5) {
            return true;
        }
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, SMS_LIMIT_TTL);
        return false;
    }

    // ========== 分布式锁 ==========

    public boolean tryLock(String key, Duration ttl) {
        String lockKey = LOCK_PREFIX + key;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String key) {
        redisTemplate.delete(LOCK_PREFIX + key);
    }

    // ========== 通用操作 ==========

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
