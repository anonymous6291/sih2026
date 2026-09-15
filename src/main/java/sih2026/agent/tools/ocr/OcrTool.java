package sih2026.agent.tools.ocr;

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
import sih2026.agent.tools.OcrAndVisionOnPdf;
import sih2026.agent.tools.Tool;
import sih2026.agent.tools.ToolManager;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;

import java.nio.file.Files;
import java.util.List;

@Service
public class OcrTool implements Tool {
    private static final String TOOL_NAME = "ocr";
    private static final String TOOL_USAGE_DESCRIPTION = """
            Tool "{name}":
            
            Always use it to extract texts from images and scanned pdf.
            
            Example:
            
            {name}:
            [documentId1, documentId2, documentId3, .....]
            
            """.replaceAll("\\{name}", TOOL_NAME);
    private static final String RUNNING_DESCRIPTION = "Performing OCR....";

    private static final String OCR_PROMPT = """
            Extract all texts from the give image.
            """;

    private final DocumentStorageManager documentStorageManager;
    private final OcrAndVisionOnPdf ocrAndVisionOnPdf;
    private final OcrAndVisionModel ocrAndVisionModel;


    OcrTool(ToolManager toolManager, DocumentStorageManager documentStorageManager, OcrAndVisionOnPdf ocrAndVisionOnPdf, OcrAndVisionModel ocrAndVisionModel) {
        this.documentStorageManager = documentStorageManager;
        this.ocrAndVisionOnPdf = ocrAndVisionOnPdf;
        this.ocrAndVisionModel = ocrAndVisionModel;
        toolManager.registerTool(TOOL_NAME, this);
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

            DocumentStorageManager.DocumentData documentData =
                    documentStorageManager.getDocumentData(username, userRole, documentId);

            if (documentData != null) {
                result
                        .append(documentData.document_name())
                        .append(" [")
                        .append(documentId)
                        .append("]:\n");

                try {
                    result.append(performTaskOnDocument(documentData));
                } catch (Exception e) {
                    IO.println(e);
                    result.append("Failed to perform ocr.");
                }
            } else {
                result.append("Document with id [").append(documentId).append("] doesn't exist.");
            }
            result.append("\n-------------------------------------------------------------\n");
        }
        return new ToolResponse(result.toString(), List.of(), false);
    }

    private String performTaskOnDocument(DocumentStorageManager.DocumentData documentData) throws Exception {
        if (documentData.mime_type().startsWith("image/")) {
            return ocrAndVisionModel.sendMessage(
                    OCR_PROMPT,
                    Files.newInputStream(
                            documentData.document_path()
                    ),
                    documentData.mime_type()
            );
        }
        if (documentData.mime_type().equals("application/pdf")) {
            return ocrAndVisionOnPdf.process(documentData.document_path(), OCR_PROMPT);
        }
        return "File with mime type [" + documentData.mime_type() + "] not supported.";
    }
}
