package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;

/** Evita repetir el "new Task<>() { call() ... }" en cada método de los servicios. */
public final class TaskUtil {

    private TaskUtil() {
    }

    public static <V> Task<V> task(Callable<V> action) {
        return new Task<>() {
            @Override
            protected V call() throws Exception {
                return action.call();
            }
        };
    }
}
