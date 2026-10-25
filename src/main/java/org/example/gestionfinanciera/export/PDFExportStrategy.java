package org.example.gestionfinanciera.export;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.example.gestionfinanciera.models.Movement;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PDFExportStrategy implements ExportStrategy {
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

        try (PdfWriter writer = new PdfWriter(Path.of(filePath).toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4.rotate())) {
            document.setMargins(24, 24, 24, 24);
            var boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            document.add(new Paragraph("Movimientos financieros")
                    .setFont(boldFont)
                    .setFontSize(14));

            Table table = new Table(UnitValue.createPercentArray(HEADERS.length))
                    .useAllAvailableWidth();
            for (String header : HEADERS) {
                table.addHeaderCell(new Cell().add(new Paragraph(header)
                        .setFont(boldFont)
                        .setFontSize(7)));
            }
            for (Movement movement : movements) {
                for (String value : values(movement)) {
                    table.addCell(new Cell().add(new Paragraph(value).setFontSize(7)));
                }
            }
            document.add(table);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo exportar el archivo PDF", e);
        }
    }

    private String[] values(Movement movement) {
        return new String[]{
                String.valueOf(movement.getMovementId()),
                String.valueOf(movement.getCategoryId()),
                String.valueOf(movement.getOriginAccountId()),
                String.valueOf(movement.getDestinationAccountId()),
                String.valueOf(movement.getUserId()),
                String.valueOf(movement.getAmount()),
                text(movement.getType()),
                text(movement.getConcept()),
                text(movement.getPaymentMethod()),
                formatDate(movement.getDate()),
                text(movement.getState()),
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
