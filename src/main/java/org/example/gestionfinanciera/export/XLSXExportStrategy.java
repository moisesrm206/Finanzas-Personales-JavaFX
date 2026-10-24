package org.example.gestionfinanciera.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.gestionfinanciera.models.Movement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class XLSXExportStrategy implements ExportStrategy {
    private static final String[] HEADERS = {
            "ID", "Categoría", "Cuenta origen", "Cuenta destino", "Usuario",
            "Importe", "Tipo", "Concepto", "Método de pago", "Fecha", "Estado", "Etiquetas"
    };

    @Override
    public void exportData(List<Movement> movements, String filePath) {
        if (movements == null) {
            throw new IllegalArgumentException("La lista de movimientos es obligatoria");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("La ruta de exportación es obligatoria");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream output = new FileOutputStream(filePath)) {
            Sheet sheet = workbook.createSheet("Movimientos");
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS[column]);
            }
            for (int rowIndex = 0; rowIndex < movements.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                String[] values = values(movements.get(rowIndex));
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column]);
                }
            }
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo exportar el archivo XLSX", e);
        }
    }

    private String[] values(Movement movement) {
        return new String[]{
                String.valueOf(movement.getMovementId()), String.valueOf(movement.getCategoryId()),
                String.valueOf(movement.getOriginAccountId()), String.valueOf(movement.getDestinationAccountId()),
                String.valueOf(movement.getUserId()), String.valueOf(movement.getAmount()),
                text(movement.getType()), text(movement.getConcept()), text(movement.getPaymentMethod()),
                formatDate(movement.getDate()), text(movement.getState()),
                String.join(", ", movement.getTags() == null ? new String[0] : movement.getTags())
        };
    }

    private String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
