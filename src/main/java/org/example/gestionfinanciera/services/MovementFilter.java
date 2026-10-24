package org.example.gestionfinanciera.services;

import org.example.gestionfinanciera.models.Movement;
import org.example.gestionfinanciera.models.Movement.PaymentMethod;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/**
 * Criterios de búsqueda de movimientos. Un valor null (o vacío) significa "sin filtro".
 * Sirve para la pantalla de movimientos y para elegir qué se exporta.
 */
public record MovementFilter(String text, String categoryId, String type, String state,
                             String tag, PaymentMethod paymentMethod, LocalDate from, LocalDate to) {

    public static MovementFilter none() {
        return new MovementFilter(null, null, null, null, null, null, null, null);
    }

    public boolean matches(Movement movement, Map<String, String> categoryNames) {
        if (isSet(categoryId) && !categoryId.equals(movement.getCategoryId())) {
            return false;
        }
        if (isSet(type) && !type.equalsIgnoreCase(movement.getType())) {
            return false;
        }
        if (isSet(state) && !state.equalsIgnoreCase(movement.getState())) {
            return false;
        }
        if (paymentMethod != null && paymentMethod != movement.getPaymentMethod()) {
            return false;
        }
        if (isSet(tag) && !hasTag(movement)) {
            return false;
        }
        LocalDate day = MovementStats.dayOf(movement);
        if (from != null && (day == null || day.isBefore(from))) {
            return false;
        }
        if (to != null && (day == null || day.isAfter(to))) {
            return false;
        }
        return !isSet(text) || matchesText(movement, categoryNames);
    }

    private boolean hasTag(Movement movement) {
        if (movement.getTags() == null) {
            return false;
        }
        String wanted = cleanTag(tag);
        for (String candidate : movement.getTags()) {
            if (wanted.equals(cleanTag(candidate))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesText(Movement movement, Map<String, String> categoryNames) {
        StringBuilder haystack = new StringBuilder();
        append(haystack, movement.getConcept());
        append(haystack, movement.getType());
        append(haystack, movement.getState());
        append(haystack, categoryNames.get(movement.getCategoryId()));
        append(haystack, movement.getPaymentMethod() == null ? null : movement.getPaymentMethod().name());
        append(haystack, String.valueOf(movement.getAmount()));
        if (movement.getTags() != null) {
            for (String movementTag : movement.getTags()) {
                append(haystack, movementTag);
            }
        }
        return haystack.toString().contains(text.trim().toLowerCase(Locale.ROOT));
    }

    private static void append(StringBuilder builder, String value) {
        if (value != null) {
            builder.append(value.toLowerCase(Locale.ROOT)).append(' ');
        }
    }

    private static String cleanTag(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim().toLowerCase(Locale.ROOT);
        return clean.startsWith("#") ? clean.substring(1) : clean;
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
