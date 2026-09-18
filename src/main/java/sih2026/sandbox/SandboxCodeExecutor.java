package sih2026.sandbox;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Service
public class SandboxCodeExecutor {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "python");
    private final DocumentStorageManager documentStorageManager;
    private final SandboxCodeExecutorContainerManager containerManager;
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final ObjectMapper jsonParser;

    public SandboxCodeExecutor(DocumentStorageManager documentStorageManager, SandboxCodeExecutorContainerManager containerManager) {
        this.documentStorageManager = documentStorageManager;
        this.containerManager = containerManager;
        jsonParser = new ObjectMapper();
        jsonParser.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Result executeCode(
            String username,
            UserRole userRole,
            String language,
            String base64_code,
            List<DocumentStorageManager.DocumentNameAndId> nameAndIds
    ) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            return new Result(true, "Language not supported.", null);
        }

        try {
            List<DocumentNameAndContent> nameAndContents = new LinkedList<>();

            for (DocumentStorageManager.DocumentNameAndId nameAndId : nameAndIds) {
                String base64Content = getDocumentBase64Content(username, userRole, nameAndId.document_id());

                if (base64Content != null) {
                    nameAndContents.add(new DocumentNameAndContent(nameAndId.document_name(), base64Content));
                }
            }

            Request request = new Request(userRole.toString(), language, base64_code, nameAndContents);

            String requestString = jsonParser.writeValueAsString(request);


            String resultString = containerManager.sendData(userRole, requestString);

            if (resultString == null) {
                return new Result(true, "Failed to execute code.", null);
            }

            return jsonParser.readValue(resultString, Result.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(true, "Failed to execute code.", null);
        }
    }

    private String getDocumentBase64Content(String username, UserRole userRole, String documentId) {
        try {
            DocumentStorageManager.DocumentNameAndPath nameAndPath
                    = documentStorageManager.getDocumentNameAndPath(username, userRole, documentId);

            if (nameAndPath == null) {
                return null;
            }

            String content = Files.readString(nameAndPath.documentPath());

            return base64Encoder.encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception _) {
            return null;
        }
    }

    public record DocumentNameAndContent(String name, String base64_content) {
    }

    public record Request(String username, String language, String base64_code,
                          List<DocumentNameAndContent> documents) {
    }

    public record ExecutorIntermediateOutput(boolean error, String base64_message) {
    }

    public record ExecutorOutput(List<ExecutorIntermediateOutput> executorIntermediateOutputs) {
    }

    public record Result(boolean error, String message, ExecutorOutput output) {
    }
}
