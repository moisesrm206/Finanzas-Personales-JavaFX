package org.example.gestionfinanciera.export;

public class ExportFactory {
    public enum Format {
        PDF, XLSX
    }

    private ExportFactory() {
    }

    public static ExportStrategy create(Format format) {
        if (format == null) {
            throw new IllegalArgumentException("El formato de exportación es obligatorio");
        }

        return switch (format) {
            case PDF -> new PDFExportStrategy();
            case XLSX -> new XLSXExportStrategy();
        };
    }
}
