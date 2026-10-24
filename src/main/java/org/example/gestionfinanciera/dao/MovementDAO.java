package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Movement;

import java.util.List;

public interface MovementDAO extends BaseDAO<Movement> {

    /** Todos los movimientos del usuario, del más reciente al más antiguo. */
    List<Movement> findByUser(String userId);

    /** Sirve para impedir que se elimine una categoría que ya tiene movimientos. */
    boolean existsByCategory(String userId, String categoryId);

    /** Sirve para impedir que se elimine una cuenta usada como origen o destino. */
    boolean existsByAccount(String userId, String accountId);
}
