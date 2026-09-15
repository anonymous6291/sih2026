package sih2026.agent.tools.vision;

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
public class VisionTool implements Tool {
    private static final String TOOL_NAME = "vision";
    private static final String TOOL_USAGE_DESCRIPTION = """
            Tool "{name}":
            
            Use it to describe the content of image and pdf with images.
            
            Example:
            
            {name}:
            [documentId1, documentId2, documentId3, .....]
            
            """.replaceAll("\\{name}", TOOL_NAME);
    private static final String RUNNING_DESCRIPTION = "Using Vision tool....";

    private static final String VISION_PROMPT = """
            Describe the given image in short. Don't miss out any critical information.
            """;

    private final DocumentStorageManager documentStorageManager;
    private final OcrAndVisionModel ocrAndVisionModel;
    private final OcrAndVisionOnPdf ocrAndVisionOnPdf;

    VisionTool(ToolManager toolManager, DocumentStorageManager documentStorageManager, OcrAndVisionModel ocrAndVisionModel, OcrAndVisionOnPdf ocrAndVisionOnPdf) {
        this.ocrAndVisionModel = ocrAndVisionModel;
        this.ocrAndVisionOnPdf = ocrAndVisionOnPdf;
        this.documentStorageManager = documentStorageManager;
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
    public ToolResponse performTask(String username, UserRole userRole, String request) throws Exception {
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

                result.append(performTaskOnDocument(documentData));
            } else {
                result.append("Document with id [").append(documentId).append("] doesn't exist.");
            }
            result.append("\n-------------------------------------------------------------\n");
        }
        return new ToolResponse(result.toString(), List.of(), false);
    }

    private String performTaskOnDocument(DocumentStorageManager.DocumentData documentData) throws Exception {
        try {
            if (documentData.mime_type().startsWith("image/")) {
                return ocrAndVisionModel.sendMessage(
                        VISION_PROMPT,
                        Files.newInputStream(
                                documentData.document_path()
                        ),
                        documentData.mime_type()
                );
            }

            if (documentData.mime_type().equals("application/pdf")) {
                return ocrAndVisionOnPdf.process(documentData.document_path(), VISION_PROMPT);
            }

            return "File with mime type [" + documentData.mime_type() + "] not supported.";
        } catch (Exception e) {
            IO.println(e);
            return "Failed to use Vision tool";
        }
    }
}
