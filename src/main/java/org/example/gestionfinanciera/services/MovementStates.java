package org.example.gestionfinanciera.services;

import java.util.List;

/** Valores permitidos para Movement.state. */
public final class MovementStates {

    public static final String APPLIED = "aplicado";
    public static final String PENDING = "pendiente";
    public static final String RECONCILED = "conciliado";

    public static final List<String> ALL = List.of(APPLIED, PENDING, RECONCILED);

    private MovementStates() {
    }
}
