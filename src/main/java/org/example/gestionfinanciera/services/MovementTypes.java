package org.example.gestionfinanciera.services;

import java.util.List;

/** Valores permitidos para Movement.type. Sirven también para llenar el ComboBox del formulario. */
public final class MovementTypes {

    public static final String INCOME = "Ingreso";
    public static final String EXPENSE = "Egreso";
    public static final String TRANSFER = "Transferencia";

    public static final List<String> ALL = List.of(INCOME, EXPENSE, TRANSFER);

    private MovementTypes() {
    }
}
