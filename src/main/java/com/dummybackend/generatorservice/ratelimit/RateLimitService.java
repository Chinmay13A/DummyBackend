package com.dummybackend.generatorservice.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public boolean tryConsume(String clientKey) {
        return resolveBucket(clientKey).tryConsume(1);
    }

    public int requestsPerMinute() {
        return properties.requestsPerMinute();
    }

    private Bucket resolveBucket(String clientKey) {
        return buckets.computeIfAbsent(clientKey, key -> createBucket());
    }

    private Bucket createBucket() {
        int limit = properties.requestsPerMinute();
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofMinutes(1)))
                .build();
    }
}
