package sih2026.agent.tools;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import sih2026.database.user.UserRole;

import java.util.LinkedList;
import java.util.List;

public interface Tool {
    String getToolName();

    String getUsageDescription();

    String getRunningDescription();

    ToolResponse performTask(String username, UserRole userRole, String request) throws Exception;

    default RequestAndDocumentId parseRequest(String request) {
        int index;

        if (!request.startsWith("[") || (index = request.indexOf("]")) == -1) {
            return new RequestAndDocumentId(request, List.of());
        }

        String docIdLists = request.substring(1, index);

        String realRequest = request.substring(index + 1);

        List<String> documentIds = new LinkedList<>();

        for (String documentId : docIdLists.split(",")) {
            if (documentId.isBlank()) {
                continue;
            }

            documentId = documentId.replaceAll("\\s*", "");

            documentId = documentId.replaceAll("\"*", "");

            documentIds.add(documentId);
        }
        return new RequestAndDocumentId(realRequest, documentIds);
    }

    record RequestAndDocumentId(String request, List<String> documentIds) {
    }

    record DocNameIdPair(String document_name, String document_id) {
    }

    record ToolResponse(String response, List<DocNameIdPair> documents, boolean stop) {
    }
}
