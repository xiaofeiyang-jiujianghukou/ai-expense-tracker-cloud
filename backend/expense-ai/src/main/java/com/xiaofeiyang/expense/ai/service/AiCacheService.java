package com.xiaofeiyang.expense.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AiCacheService {

    private static final String ANALYSIS_PREFIX = "ai:analysis:";
    private static final String REPORT_PREFIX = "ai:report:";
    private static final String CATEGORIZE_PREFIX = "ai:categorize:";
    private static final Duration ANALYSIS_TTL = Duration.ofHours(1);
    private static final Duration REPORT_TTL = Duration.ofHours(6);
    private static final Duration CATEGORIZE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public String getAnalysis(Long userId, int year, int month) {
        return redisTemplate.opsForValue().get(analysisKey(userId, year, month));
    }

    public void putAnalysis(Long userId, int year, int month, String value) {
        redisTemplate.opsForValue().set(analysisKey(userId, year, month), value, ANALYSIS_TTL);
    }

    public String getReport(Long userId, int year, int month) {
        return redisTemplate.opsForValue().get(reportKey(userId, year, month));
    }

    public void putReport(Long userId, int year, int month, String value) {
        redisTemplate.opsForValue().set(reportKey(userId, year, month), value, REPORT_TTL);
    }

    public String getCategorize(Long userId, String description, String type) {
        return redisTemplate.opsForValue().get(categorizeKey(userId, description, type));
    }

    public void putCategorize(Long userId, String description, String type, String value) {
        redisTemplate.opsForValue().set(categorizeKey(userId, description, type), value, CATEGORIZE_TTL);
    }

    private static String analysisKey(Long userId, int year, int month) {
        return ANALYSIS_PREFIX + userId + ":" + year + ":" + month;
    }

    private static String reportKey(Long userId, int year, int month) {
        return REPORT_PREFIX + userId + ":" + year + ":" + month;
    }

    private static String categorizeKey(Long userId, String description, String type) {
        // Hash description to avoid special chars in Redis key
        int descHash = description.trim().hashCode();
        return CATEGORIZE_PREFIX + userId + ":" + type + ":" + descHash;
    }
}
