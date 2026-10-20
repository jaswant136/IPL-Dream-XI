package com.ipldraftclub.security;

/**
 * Turns a bearer token into an identity or throws {@link InvalidTokenException}.
 * Production uses {@link FirebaseTokenVerifier}; automated tests supply a test-only implementation
 * so no real Firebase credentials are needed (see src/test/.../TestSecurityConfig).
 */
public interface TokenVerifier {

    AuthenticatedUser verify(String bearerToken) throws InvalidTokenException;
}
