package com.ipldraftclub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /** Source of randomness for the spin. A bean so tests can substitute a deterministic generator. */
    @Bean
    public RandomGenerator spinRandom() {
        return new SecureRandom();
    }
}
