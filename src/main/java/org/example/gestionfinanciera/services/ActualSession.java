package org.example.gestionfinanciera.services;

import org.example.gestionfinanciera.models.User;

/** Singleton que guarda el usuario que inició sesión mientras la aplicación está abierta. */
public class ActualSession {

    private static ActualSession instance;

    // volatile: los Task de JavaFX leen este valor desde otros hilos
    private volatile User user;

    private ActualSession() {
    }

    public static synchronized ActualSession getInstance() {
        if (instance == null) {
            instance = new ActualSession();
        }
        return instance;
    }

    /** Se llama justo después de autenticar o registrar al usuario. */
    public void iniciar(User user) {
        this.user = user;
    }

    /** Se llama al cerrar sesión. */
    public void cerrar() {
        this.user = null;
    }

    public boolean haySesion() {
        return user != null;
    }

    public User getUser() {
        return user;
    }

    /** Id que se guarda como "userId" en cada movimiento, categoría, cuenta y presupuesto. */
    public String getUserId() {
        if (user == null) {
            throw new IllegalStateException("No hay una sesión iniciada");
        }
        return user.getId();
    }
}