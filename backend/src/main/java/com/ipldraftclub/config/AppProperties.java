package com.ipldraftclub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Firebase firebase, Security security, Seed seed, RateLimit rateLimit) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Firebase(boolean enabled, String projectId, String serviceAccountPath) {
    }

    public record Security(boolean requireEmailVerified) {
    }

    public record Seed(boolean enabled, String csvPath) {
    }

    public record RateLimit(int startDraftPerMinute) {
    }
}
