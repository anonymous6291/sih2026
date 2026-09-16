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
import sih2026.agent.tools.Tool;
import sih2026.agent.tools.ToolManager;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentParserTool implements Tool {
    private static final String TOOL_NAME = "read";
    private static final String TOOL_USAGE_DESCRIPTION = """
            Tool "{name}":
            
            Always use it to read content of documents.
            
            Example:
            
            {name}:
            [documentId1, documentId2, documentId3,.....]
            
            """.replaceAll("\\{name}", TOOL_NAME);
    private static final String RUNNING_DESCRIPTION = "Reading documents....";

    private final DocumentStorageManager documentStorageManager;
    private final ConcurrentHashMap<String, DocumentParser> documentParsers = new ConcurrentHashMap<>();

    public DocumentParserTool(
            ToolManager toolManager,
            DocumentStorageManager documentStorageManager
    ) {
        this.documentStorageManager = documentStorageManager;
        toolManager.registerTool(TOOL_NAME, this);
    }

    public void registerDocumentParser(String mimeType, DocumentParser documentParser) {
        if (documentParsers.containsKey(mimeType)) {
            throw new IllegalArgumentException("Document parser for mimeType [" + mimeType + "] already exists.");
        }
        documentParsers.put(mimeType, documentParser);
    }


    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getUsageDescription() {
        return TOOL_USAGE_DESCRIPTION;
    }

    @Override
    public String getRunningDescription() {
        return RUNNING_DESCRIPTION;
    }

    @Override
    public ToolResponse performTask(String username, UserRole userRole, String request) {
        RequestAndDocumentId requestAndDocumentId = parseRequest(request);

        if (requestAndDocumentId.documentIds().isEmpty()) {
            return new ToolResponse("Invalid tool usage format.", List.of(), false);
        }

        StringBuilder result = new StringBuilder();

        for (String documentId : requestAndDocumentId.documentIds()) {

            DocumentStorageManager.DocumentData documentData
                    = documentStorageManager.getDocumentData(username, userRole, documentId);

            if (documentData != null) {
                result.append(documentData.document_name())
                        .append(" [")
                        .append(documentId)
                        .append("]:\n");

                try {
                    String content = readFile(documentData.document_path(), documentData.mime_type());
                    if (content == null) {
                        result.append("File with mime type [").append(documentData.mime_type()).append("] is not supported.");
                    } else {
                        result.append(content);
                    }
                } catch (Exception e) {
                    result.append("Failed to read the content.");
                }
            } else {
                result.append("Document with id [").append(documentId).append("] doesn't exist.");
            }
            result.append("\n-------------------------------------------------------------\n");
        }

        return new ToolResponse(result.toString(), List.of(), false);
    }

    public String readFile(Path documentPath, String mimeType) throws Exception {
        DocumentParser documentParser = documentParsers.get(mimeType);

        if (documentParser != null) {
            return documentParser.getText(mimeType, documentPath);
        }

        for (DocumentParser anyParser : documentParsers.values()) {
            if (anyParser.supportsMimeType(mimeType)) {
                return anyParser.getText(mimeType, documentPath);
            }
        }
        return null;
    }
}
