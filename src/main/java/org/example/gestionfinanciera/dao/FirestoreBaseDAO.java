package org.example.gestionfinanciera.dao;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.example.gestionfinanciera.database.config.DBconnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * CRUD común sobre una colección de Firestore. Cada DAO concreto solo indica cómo convertir
 * su entidad a documento y viceversa (igual que hace userDAOImpl).
 */
public abstract class FirestoreBaseDAO<T> implements BaseDAO<T> {

    private static final int MAX_BATCH = 400; // Firestore permite 500 operaciones por lote

    private final Firestore db;
    protected final CollectionReference collection;

    protected FirestoreBaseDAO(String collectionName) {
        this.db = DBconnection.getInstance().getDb();
        this.collection = db.collection(collectionName);
    }

    protected abstract Map<String, Object> toDocument(T entity);

    protected abstract T fromSnapshot(DocumentSnapshot snapshot);

    protected abstract String getId(T entity);

    protected abstract void setId(T entity, String id);

    @Override
    public T create(T entity) {
        DocumentReference document = collection.document();
        setId(entity, document.getId());
        await(document.set(toDocument(entity)));
        return entity;
    }

    @Override
    public void createAll(List<T> entities) {
        WriteBatch batch = db.batch();
        int pending = 0;
        for (T entity : entities) {
            DocumentReference document = collection.document();
            setId(entity, document.getId());
            batch.set(document, toDocument(entity));
            pending++;
            if (pending >= MAX_BATCH) {
                await(batch.commit());
                batch = db.batch();
                pending = 0;
            }
        }
        if (pending > 0) {
            await(batch.commit());
        }
    }

    @Override
    public Optional<T> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        DocumentSnapshot snapshot = await(collection.document(id).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(fromSnapshot(snapshot));
    }

    @Override
    public void update(T entity) {
        String id = getId(entity);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("No se puede actualizar un registro sin ID");
        }
        await(collection.document(id).set(toDocument(entity)));
    }

    @Override
    public void delete(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("No se puede eliminar un registro sin ID");
        }
        await(collection.document(id).delete());
    }

    @Override
    public void deleteAll(Collection<String> ids) {
        WriteBatch batch = db.batch();
        int pending = 0;
        for (String id : ids) {
            batch.delete(collection.document(id));
            pending++;
            if (pending >= MAX_BATCH) {
                await(batch.commit());
                batch = db.batch();
                pending = 0;
            }
        }
        if (pending > 0) {
            await(batch.commit());
        }
    }

    /** Ejecuta una consulta y convierte los documentos en objetos. */
    protected List<T> query(Query query) {
        List<T> result = new ArrayList<>();
        for (QueryDocumentSnapshot snapshot : await(query.get()).getDocuments()) {
            result.add(fromSnapshot(snapshot));
        }
        return result;
    }

    protected <R> R await(ApiFuture<R> future) {
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
