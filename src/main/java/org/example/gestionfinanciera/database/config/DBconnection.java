package org.example.gestionfinanciera.database.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

public class DBconnection {
    private static DBconnection instance;
    private final Firestore db;

    private DBconnection() {
        try (InputStream in = getClass().getResourceAsStream("/finances-management-40f84-firebase-adminsdk-fbsvc-a6e2a57099.json")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró serviceAccountKey.json en resources");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            db = FirestoreClient.getFirestore();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo conectar con Firebase", e);
        }
    }

    public static synchronized DBconnection getInstance() {
        if (instance == null) instance = new DBconnection();
        return instance;
    }

    public Firestore getDb() { return db; }
}