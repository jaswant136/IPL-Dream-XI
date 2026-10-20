package com.ipldraftclub.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Production verifier: validates the signature, issuer, audience and expiry of a Firebase ID token. */
@Component
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseTokenVerifier implements TokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenVerifier.class);

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenVerifier(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public AuthenticatedUser verify(String bearerToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(bearerToken);
            return new AuthenticatedUser(token.getUid(), token.getEmail(), token.getName(), token.isEmailVerified());
        } catch (FirebaseAuthException | IllegalArgumentException | IllegalStateException e) {
            // Log the category only - never the token itself.
            log.debug("Firebase ID token rejected: {}", e.getClass().getSimpleName());
            throw new InvalidTokenException("Invalid or expired authentication token.", e);
        }
    }
}
