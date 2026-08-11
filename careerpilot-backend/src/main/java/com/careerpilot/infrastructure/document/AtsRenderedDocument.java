package com.careerpilot.infrastructure.document;

/**
 * Output record from AtsDocumentRenderer.
 * Holds the raw bytes for both rendered formats.
 * Immutable — use the static factory method.
 */
public record AtsRenderedDocument(byte[] pdfBytes, byte[] docxBytes) {

    public static AtsRenderedDocument of(byte[] pdfBytes, byte[] docxBytes) {
        return new AtsRenderedDocument(pdfBytes, docxBytes);
    }
}
