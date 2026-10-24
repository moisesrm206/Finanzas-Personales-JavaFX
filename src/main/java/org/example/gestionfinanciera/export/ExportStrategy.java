package org.example.gestionfinanciera.export;

import org.example.gestionfinanciera.models.Movement;

import java.util.List;

public interface ExportStrategy {
    void exportData(List<Movement> movements, String filePath);
}
