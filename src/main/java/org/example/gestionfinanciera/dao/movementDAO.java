package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Movement;

public interface movementDAO {
    Movement getMovementById(int id);
    void saveMovement(Movement movement);
    void updateMovement(Movement movement);
    void deleteMovement(Movement movement);
}
