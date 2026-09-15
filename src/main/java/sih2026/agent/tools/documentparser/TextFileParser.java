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

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Service
public class TextFileParser implements DocumentParser {
    private static final Set<String> supportedMimeTypes =
            Set.of(
                    "text/plain",
                    "text/html",
                    "application/json",
                    "application/yaml",
                    "text/javascript",
                    "text/css",
                    "text/x-java-source"
            );

    TextFileParser(DocumentParserTool documentParserTool) {
        for (String mimeType : supportedMimeTypes) {
            documentParserTool.registerDocumentParser(mimeType, this);
        }
    }

    public String readFile(Path filePath) throws Exception {
        try (BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
            return bufferedReader.readAllAsString();
        }
    }

    @Override
    public boolean supportsMimeType(String mimeType) {
        return supportedMimeTypes.contains(mimeType) || mimeType.startsWith("text/");
    }

    @Override
    public String getText(String mimeType, Path filePath) throws Exception {
        if (!supportsMimeType(mimeType)) {
            throwUnsupportedMimeTypeException(mimeType);
        }
        return readFile(filePath);
    }
}
