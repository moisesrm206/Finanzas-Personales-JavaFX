package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.dao.userDAO;
import org.example.gestionfinanciera.dao.userDAOImpl;
import org.example.gestionfinanciera.models.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

public class userServiceImpl implements userService {

    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos.";
    private final userDAO userdao ;

    public userServiceImpl(userDAO userdao) {
        this.userdao = new userDAOImpl();
    }

    @Override
    public Task<Void> addUserTask(User user) {
        return new Task<>() {
            @Override
            protected Void call() {
                userdao.saveUser(user);
                return null;
            }
        };
    }

    @Override
    public Task<List<User>> loadUsersTask() {
        return new Task<>() {
            @Override
            protected List<User> call() {
                return userdao.getUsers();
            }
        };
    }

    @Override
    public Task<User> authenticateTask(String username, String password) {
        return new Task<>() {
            @Override
            protected User call() throws AuthException {
            User user = userdao.getUserByName(username)
                        .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS));

                if (!verifyPassword(password, user.getPassword())) {
                    throw new AuthException(INVALID_CREDENTIALS);
                }
                return user;
            }
        };
    }

    @Override
    public Task<User> registerTask(String username, String password) {
        return new Task<>() {
            @Override
            protected User call() throws AuthException {
                if (userdao.getUserByName(username).isPresent()) {
                    throw new AuthException("Ese nombre de usuario ya está en uso.");
                }
                User user = new User(username, password);
                userdao.saveUser(user);
                return user;
            }
        };
    }

    private boolean verifyPassword(String password, String storedPassword) throws AuthException {
        if (storedPassword == null || !storedPassword.startsWith("pbkdf2_sha256$")) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        try {
            String[] parts = storedPassword.split("\\$");
            if (parts.length != 4) {
                throw new AuthException(INVALID_CREDENTIALS);
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            PBEKeySpec keySpec = new PBEKeySpec(
                    password.toCharArray(), salt, iterations, expectedHash.length * 8
            );
            byte[] actualHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            throw new AuthException(INVALID_CREDENTIALS);
        }
    }


    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}
