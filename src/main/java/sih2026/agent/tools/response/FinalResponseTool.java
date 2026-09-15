package sih2026.agent.tools.response;

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

import java.util.LinkedList;
import java.util.List;

@Service
public class FinalResponseTool implements Tool {
    private static final String TOOL_NAME = "response";
    private static final String TOOL_USAGE_DESCRIPTION = """
            Tool "{name}":
            
            Use it when you have arrived at final decision, have nothing more to perform or operate on and want to return the result or report.
            To attach the documents in the response, put the document id of the documents as a list in the next line after tool name "{name}".
            If you don't want to attach any document then the list must be empty, not missing.
            
            Usage format:
            
            {name}:
            [documentId1, documentId2, documentId3, ....]
            Your final response.
            
            Example 1:
            
            {name}:
            [documentId1, documentId2, documentId3]
            These are the generated documents that contains all the reports.
            
            Example 2:
            
            {name}:
            []
            Java is a high level programming language. It's memory safe and has automatic garbage collection.
            """.replaceAll("\\{name}", TOOL_NAME);
    private static final String RUNNING_DESCRIPTION = "Generating output....";

    private final DocumentStorageManager documentStorageManager;

    FinalResponseTool(ToolManager toolManager, DocumentStorageManager documentStorageManager) {
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

        String response = requestAndDocumentId.request();

        List<DocNameIdPair> docNameIdPairs = new LinkedList<>();

        for (String documentId : requestAndDocumentId.documentIds()) {

            DocumentStorageManager.DocumentNameAndPath documentNameAndPath =
                    documentStorageManager.getDocumentNameAndPath(username, userRole, documentId);

            if (documentNameAndPath != null) {
                docNameIdPairs.add(
                        new DocNameIdPair(
                                documentNameAndPath.documentName(),
                                documentId
                        )
                );
            }
        }
        return new ToolResponse(response, docNameIdPairs, true);
    }
}
