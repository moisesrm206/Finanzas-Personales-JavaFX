package org.example.gestionfinanciera.services;

/** Totales mensuales de un año para el gráfico de líneas. El índice 0 es enero y el 11 es diciembre. */
public record MonthlyTotals(double[] income, double[] expenses) {
}
