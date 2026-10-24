package org.example.gestionfinanciera.services;

import org.example.gestionfinanciera.export.ExportFactory;
import org.example.gestionfinanciera.export.ExportStrategy;
import org.example.gestionfinanciera.models.Movement;

import java.io.File;
import java.util.List;

public class ExportService {
    public void export(List<Movement> movements, String filePath, ExportFactory.Format format) {
        File carpeta = new File(filePath).getAbsoluteFile().getParentFile();
        if (carpeta != null) {
            carpeta.mkdirs(); // crea la carpeta (y las anteriores) si no existe
        }
        ExportStrategy strategy = ExportFactory.create(format);
        strategy.exportData(movements, filePath);
    }
}
