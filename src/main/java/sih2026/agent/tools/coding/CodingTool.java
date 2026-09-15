package sih2026.agent.tools.coding;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sih2026.agent.tools.Tool;
import sih2026.agent.tools.ToolManager;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

@Service
public class CodingTool implements Tool {
    private static final String TOOL_NAME = "code";
    private static final String TOOL_USAGE_DESCRIPTION = """
            Tool "{name}":
            
            It can write code or generate documents. Only use it to write code, perform calculations and generate documents.
            It cannot read documents so you have to provide the content of the documents.
            
            Example 1:
            
            {name}:
            []
            Write a code to find the mean of the dataset [1, 2, 3, 88, 0, 8898].
            
            Example 2:
            
            {name}:
            []
            Generate a pdf file containing the data "Java is a programming language. Java is a high level programming language......"
            
            Example 3:
            
            {name}:
            []
            Generate a csv with row1 data = [Semester, 1, 2, 3, 4] and row2 data = [Marks, 10, 8, 7, 3].
            
            """.replaceAll("\\{name}", TOOL_NAME);
    private static final String RUNNING_DESCRIPTION = "Using Coding tool....";
    private final ObjectMapper jsonParser;
    private final DocumentStorageManager documentStorageManager;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    @Value("${coding_tool.url}")
    private String codingToolUrl;

    CodingTool(ToolManager toolManager, DocumentStorageManager documentStorageManager) {
        this.documentStorageManager = documentStorageManager;
        toolManager.registerTool(TOOL_NAME, this);
        jsonParser = new ObjectMapper();
        jsonParser.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
        try {
            RequestAndDocumentId requestAndDocumentId = parseRequest(request);

            String query = requestAndDocumentId.request();

            List<DocumentNameAndData> nameAndDataList = new LinkedList<>();

            for (String documentId : requestAndDocumentId.documentIds()) {

                DocumentNameAndData documentNameAndData = getDocumentNameData(username, userRole, documentId);

                if (documentNameAndData != null) {
                    nameAndDataList.add(documentNameAndData);
                }
            }

            CodingToolRequest codingToolRequest = new CodingToolRequest(query, nameAndDataList);

            CodingToolResponse codingToolResponse = processCodingToolRequest(codingToolRequest);

            return processCodingToolResponse(username, userRole, codingToolResponse);
        } catch (Exception e) {
            return new ToolResponse("Error occurred while using the tool.", List.of(), false);
        }
    }

    private DocumentNameAndData getDocumentNameData(String username, UserRole userRole, String documentId) throws Exception {
        DocumentStorageManager.DocumentNameAndPath documentNameAndPath =
                documentStorageManager.getDocumentNameAndPath(username, userRole, documentId);

        if (documentNameAndPath == null) {
            return null;
        }

        String encodedData =
                base64Encoder.encodeToString(
                        Files.readAllBytes(documentNameAndPath.documentPath())
                );

        return new DocumentNameAndData(documentNameAndPath.documentName(), encodedData);
    }

    private CodingToolResponse processCodingToolRequest(CodingToolRequest codingToolRequest) throws Exception {
        String requestJson = jsonParser.writeValueAsString(codingToolRequest);

        HttpRequest httpRequest = HttpRequest
                .newBuilder(URI.create(codingToolUrl))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> httpResponse = httpClient
                .send(
                        httpRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        String responseJson = httpResponse.body();

        return jsonParser.readValue(responseJson, CodingToolResponse.class);
    }

    private ToolResponse processCodingToolResponse(String username, UserRole userRole, CodingToolResponse codingToolResponse) {
        List<DocNameIdPair> nameIdPairs = new LinkedList<>();

        for (DocumentNameAndData nameAndData : codingToolResponse.documents()) {
            String documentName = nameAndData.document_name();

            InputStream inputStream = new ByteArrayInputStream(
                    base64Decoder.decode(nameAndData.base64_data())
            );

            DocumentStorageManager.DocumentNameAndId documentNameAndId =
                    documentStorageManager.storeDocument(username, userRole, documentName, inputStream);

            nameIdPairs.add(
                    new DocNameIdPair(
                            documentName,
                            documentNameAndId.document_id()
                    )
            );
        }

        return new ToolResponse(codingToolResponse.response(), nameIdPairs, false);
    }

    record DocumentNameAndData(String document_name, String base64_data) {
    }

    record CodingToolRequest(String query, List<DocumentNameAndData> documents) {
    }

    record CodingToolResponse(boolean error, String response, List<DocumentNameAndData> documents) {
    }
}
