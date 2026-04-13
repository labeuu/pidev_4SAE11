package com.esprit.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "notification.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${notification.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream stream = credentialsStream();
            if (stream == null) {
                throw new IllegalStateException(
                    "Firebase credentials not set. Set GOOGLE_APPLICATION_CREDENTIALS env var or notification.firebase.credentials-path");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    private InputStream credentialsStream() throws IOException {
        if (StringUtils.hasText(credentialsPath)) {
            File f = new File(credentialsPath.trim());
            if (f.isFile()) {
                return new FileInputStream(f);
            }
        }
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(envPath)) {
            File f = new File(envPath.trim());
            if (f.isFile()) {
                return new FileInputStream(f);
            }
        }
        // Repo: <root>/firebase-credentials/*firebase-adminsdk*.json (gitignored). Walk up from user.dir to find it.
        File credsDir = findFirebaseCredentialsDir();
        if (credsDir != null) {
            File[] files = credsDir.listFiles(
                (d, n) -> n != null && n.endsWith(".json") && n.contains("firebase-adminsdk"));
            if (files != null && files.length > 0) {
                return new FileInputStream(files[0]);
            }
        }
        return null;
    }

    /** Resolves firebase-credentials/ whether the JVM was started from repo root, Notification/, etc. */
    private static File findFirebaseCredentialsDir() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File candidate = new File(dir, "firebase-credentials");
            if (candidate.isDirectory()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }
}
