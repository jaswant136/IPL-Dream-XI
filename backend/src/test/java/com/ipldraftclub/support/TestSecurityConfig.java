package com.ipldraftclub.support;

import com.ipldraftclub.security.AuthenticatedUser;
import com.ipldraftclub.security.InvalidTokenException;
import com.ipldraftclub.security.TokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * TEST-ONLY authentication. Lives in src/test so it can never ship with the application.
 * A bearer token of the form "test-&lt;uid&gt;" authenticates as that uid; anything else is rejected,
 * which lets integration tests exercise the real security filter chain without Firebase credentials.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public TokenVerifier testTokenVerifier() {
        return token -> {
            if (token != null && token.startsWith("test-") && token.length() > 5) {
                String uid = token.substring(5);
                return new AuthenticatedUser(uid, uid + "@example.test", "Tester " + uid, true);
            }
            throw new InvalidTokenException("rejected by test verifier");
        };
    }
}
