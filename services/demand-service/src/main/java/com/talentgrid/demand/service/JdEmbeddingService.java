package com.talentgrid.demand.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdEmbeddingService {

    private final GeminiEmbeddingService geminiEmbeddingService;
    private final RedisTemplate<String, float[]> embeddingRedisTemplate;

    @Value("${ai.embedding.jd-cache-ttl-seconds:86400}")
    private long jdCacheTtl;

    public float[] embedWithCache(String jobDescriptionText) {
        if (jobDescriptionText == null || jobDescriptionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Job description text cannot be null or empty");
        }
        
        String hash = computeSha256(jobDescriptionText.trim());
        String redisKey = "embedding:jd:" + hash;

        float[] cachedEmbedding = embeddingRedisTemplate.opsForValue().get(redisKey);
        if (cachedEmbedding != null) {
            log.info("JD embedding cache hit for hash: {}", hash);
            return cachedEmbedding;
        }

        log.info("JD embedding cache miss. Generating new embedding...");
        float[] newEmbedding = geminiEmbeddingService.embedContent(jobDescriptionText.trim());
        
        embeddingRedisTemplate.opsForValue().set(redisKey, newEmbedding, jdCacheTtl, TimeUnit.SECONDS);
        log.debug("Saved JD embedding to Redis cache with key: {}", redisKey);
        
        return newEmbedding;
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
