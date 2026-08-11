package com.careerpilot.infrastructure.document;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class DocumentParser {

    private final Tika tika;

    public DocumentParser() {
        this.tika = new Tika();
    }

    public String detectMimeType(InputStream stream, String filename) throws Exception {
        return tika.detect(stream, filename);
    }

    public String detectMimeType(byte[] bytes, String filename) {
        return tika.detect(bytes, filename);
    }

    public String parseToString(InputStream stream) throws Exception {
        return tika.parseToString(stream);
    }
}
