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

import org.springframework.stereotype.Service;
import sih2026.agent.model.vision.OcrAndVisionModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Service
public class TextFromImageParser implements DocumentParser {
    private static final String OCR_PROMPT = """
            Extract all texts from given image.
            """;
    private static final Set<String> supportedMimeTypes
            = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/tiff",
            "image/webp",
            "image/svg+xml",
            "image/x-icon"
    );
    private final OcrAndVisionModel ocrAndVisionModel;

    public TextFromImageParser(DocumentParserTool documentParserTool, OcrAndVisionModel ocrAndVisionModel) {
        this.ocrAndVisionModel = ocrAndVisionModel;
        for (String mimeType : supportedMimeTypes) {
            documentParserTool.registerDocumentParser(mimeType, this);
        }
    }

    @Override
    public boolean supportsMimeType(String mimeType) {
        return supportedMimeTypes.contains(mimeType) || mimeType.startsWith("image/");
    }

    @Override
    public String getText(String mimeType, Path filePath) throws Exception {
        if (!supportsMimeType(mimeType)) {
            throwUnsupportedMimeTypeException(mimeType);
        }
        return ocrAndVisionModel.sendMessage(OCR_PROMPT, Files.newInputStream(filePath), mimeType);
    }
}
