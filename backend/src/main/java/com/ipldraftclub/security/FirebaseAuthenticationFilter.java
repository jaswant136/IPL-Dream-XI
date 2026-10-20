package com.ipldraftclub.security;

import com.ipldraftclub.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer &lt;Firebase ID token&gt;", verifies it server-side and establishes the principal.
 * A missing/invalid token simply leaves the request anonymous; protected routes then get a 401 from
 * the authentication entry point, while public routes keep working.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "ipldraftclub.auth.error";
    private static final String PREFIX = "Bearer ";

    private final TokenVerifier verifier;
    private final UserService userService;
    private final boolean requireEmailVerified;

    public FirebaseAuthenticationFilter(TokenVerifier verifier, UserService userService, boolean requireEmailVerified) {
        this.verifier = verifier;
        this.userService = userService;
        this.requireEmailVerified = requireEmailVerified;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                AuthenticatedUser user = verifier.verify(token);
                if (requireEmailVerified && !user.emailVerified()) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Please verify your email address to continue.");
                } else {
                    userService.ensureProfile(user);
                    var auth = new UsernamePasswordAuthenticationToken(user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (InvalidTokenException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid or expired authentication token.");
            }
        }
        chain.doFilter(request, response);
    }
}
