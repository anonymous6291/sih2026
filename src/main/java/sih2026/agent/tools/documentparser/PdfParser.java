package sih2026.agent.tools.documentparser;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import sih2026.agent.tools.OcrAndVisionOnPdf;

import java.nio.file.Path;
import java.util.Set;

@Service
public class PdfParser implements DocumentParser {
    private static final Set<String> supportedMimeTypes = Set.of("application/pdf");
    private static final String OCR_PROMPT = """
            Extract all the texts from the given image.
            """;
    private final OcrAndVisionOnPdf ocrAndVisionOnPdf;
    private final Tika tika = new Tika();

    public PdfParser(DocumentParserTool documentParserTool, OcrAndVisionOnPdf ocrAndVisionOnPdf) {
        this.ocrAndVisionOnPdf = ocrAndVisionOnPdf;
        for (String mimeType : supportedMimeTypes) {
            documentParserTool.registerDocumentParser(mimeType, this);
        }
    }


    @Override
    public boolean supportsMimeType(String mimeType) {
        return supportedMimeTypes.contains(mimeType);
    }

    @Override
    public String getText(String mimeType, Path filePath) throws Exception {
        if (!supportsMimeType(mimeType)) {
            throwUnsupportedMimeTypeException(mimeType);
        }

        String text = tika.parseToString(filePath);

        if (text.length() > 20) {
            return text;
        }

        return ocrAndVisionOnPdf.process(filePath, OCR_PROMPT);
    }
}
