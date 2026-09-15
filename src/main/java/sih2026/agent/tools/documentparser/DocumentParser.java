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

import java.nio.file.Path;

public interface DocumentParser {
    boolean supportsMimeType(String mimeType);

    String getText(String mimeType, Path filePath) throws Exception;

    default void throwUnsupportedMimeTypeException(String mimeType) {
        throw new IllegalArgumentException("Unsupported mimeType [" + mimeType + "].");
    }
}
