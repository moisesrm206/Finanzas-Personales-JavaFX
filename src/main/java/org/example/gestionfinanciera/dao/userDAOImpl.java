package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.database.config.DBconnection;
import org.example.gestionfinanciera.models.User;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.util.HashMap;
import java.util.Map;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.concurrent.ExecutionException;

public class userDAOImpl implements userDAO{

    private static final String USERS_COLLECTION = "users";
    private static final String PASSWORD_PREFIX = "pbkdf2_sha256$";
    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Firestore db;

    public userDAOImpl() {
        this.db = DBconnection.getInstance().getDb();
    }

    @Override
    public User getUserById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }
        DocumentSnapshot snapshot = getDocument(id);
        if (!snapshot.exists()) {
            return null;
        }

        return new User(
                snapshot.getId(),
                snapshot.getString("name"),
                snapshot.getString("password")
        );
    }

    @Override
    public void saveUser(User user) {
        validateUser(user);
        DocumentReference document = db.collection(USERS_COLLECTION).document();
        user.setId(document.getId());
        await(document.set(toDocument(user)));
    }

    @Override
    public void updateUser(User user) {
        validateUser(user);
        validateId(user);
        await(usersDocument(user.getId()).set(toDocument(user)));
    }

    @Override
    public void deleteUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }
        validateId(user);
        await(usersDocument(user.getId()).delete());
    }

    private DocumentSnapshot getDocument(String id) {
        return await(usersDocument(id).get());
    }

    private DocumentReference usersDocument(String id) {
        return db.collection(USERS_COLLECTION).document(id);
    }

    private Map<String, Object> toDocument(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("password", hashIfNeeded(user.getPassword()));
        return data;
    }

    private String hashIfNeeded(String password) {
        if (password.startsWith(PASSWORD_PREFIX)) {
            return password;
        }

        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        try {
            PBEKeySpec keySpec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    PASSWORD_ITERATIONS,
                    HASH_LENGTH
            );
            byte[] hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            return PASSWORD_PREFIX
                    + PASSWORD_ITERATIONS
                    + "$"
                    + Base64.getEncoder().encodeToString(salt)
                    + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña", e);
        }
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }

    private void validateId(User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            throw new IllegalArgumentException("El usuario debe tener un ID de Firebase");
        }
    }

    private <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La operación con Firebase fue interrumpida", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("No se pudo completar la operación con Firebase", e.getCause());
        }
    }
}