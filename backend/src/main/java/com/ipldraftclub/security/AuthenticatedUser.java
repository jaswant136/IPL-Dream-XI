package com.ipldraftclub.security;

/**
 * The identity established by a verified Firebase ID token. This is the ONLY source of the caller's
 * UID: request bodies and parameters are never trusted for identity.
 */
public record AuthenticatedUser(String uid, String email, String name, boolean emailVerified) {
}
