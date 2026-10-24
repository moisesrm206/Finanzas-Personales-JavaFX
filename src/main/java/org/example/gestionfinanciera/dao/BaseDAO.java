package org.example.gestionfinanciera.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Operaciones CRUD comunes a todas las entidades. */
public interface BaseDAO<T> {

    /** Guarda la entidad con un id nuevo y la devuelve con ese id asignado. */
    T create(T entity);

    /** Guarda varias entidades en lotes (rápido para datos de prueba); les asigna su id. */
    void createAll(List<T> entities);

    Optional<T> findById(String id);

    /** Reemplaza el documento existente; la entidad debe tener id. */
    void update(T entity);

    void delete(String id);

    void deleteAll(Collection<String> ids);
}
