package com.ipldraftclub.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Firebase Admin SDK setup (server side only).
 *
 * Credential resolution order:
 *  1. FIREBASE_SERVICE_ACCOUNT_PATH - a service-account JSON file kept OUTSIDE version control (local dev).
 *  2. Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS, Cloud Run / GKE / GCE metadata, ...).
 *  3. Verify-only mode: with just FIREBASE_PROJECT_ID the SDK can still verify ID tokens (it only needs
 *     Google's public signing keys), but cannot call privileged Admin APIs.
 */
@Configuration
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp(AppProperties props) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        AppProperties.Firebase cfg = props.firebase();
        FirebaseOptions.Builder builder = FirebaseOptions.builder();

        if (StringUtils.hasText(cfg.serviceAccountPath())) {
            try (InputStream in = new FileInputStream(cfg.serviceAccountPath())) {
                builder.setCredentials(GoogleCredentials.fromStream(in));
            }
            log.info("Firebase Admin initialised from the configured service-account file.");
        } else {
            try {
                builder.setCredentials(GoogleCredentials.getApplicationDefault());
                log.info("Firebase Admin initialised with Application Default Credentials.");
            } catch (IOException e) {
                log.warn("No Firebase service account or Application Default Credentials found. "
                        + "Running in verify-only mode: ID tokens can be verified, nothing else.");
                Date farFuture = new Date(System.currentTimeMillis() + 10L * 365 * 24 * 3600 * 1000);
                builder.setCredentials(GoogleCredentials.create(new AccessToken("verify-only", farFuture)));
            }
        }

        if (StringUtils.hasText(cfg.projectId())) {
            builder.setProjectId(cfg.projectId());
        } else {
            log.warn("FIREBASE_PROJECT_ID is not set. Token verification only works if the credentials "
                    + "embed a project id. Set FIREBASE_PROJECT_ID to your Firebase project id.");
        }
        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }
}
