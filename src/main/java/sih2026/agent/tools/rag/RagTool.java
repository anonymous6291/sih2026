package sih2026.agent.tools.rag;

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
import sih2026.database.user.UserRole;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class RagTool implements Tool {
    private static final String TOOL_NAME = "rag";
    private static final String TOOL_USAGE_DESCRIPTION = """
            Tool "{name}":
            
            Always use it to retrieve data chunks from knowledge database (RAG).
            
            Usage format:
            
            {name}:
            []
            Keywords or texts for similarity checking in the knowledge database (RAG).
            
            Example:
            
            {name}:
            []
            encryption key hash function SHA256 AES256
            """.replaceAll("\\{name}", TOOL_NAME);

    private static final String RUNNING_DESCRIPTION = "Using knowledge base....";

    private static final String QUERY_SUB_URL = "/query";

    private static final String UPLOAD_SUB_URL = "/upload";

    private static final String DELETE_SUB_URL = "/delete";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    private final ObjectMapper jsonParser = new ObjectMapper();

    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    @Value("${rag_tool.url}")
    private String ragUrl;

    RagTool(ToolManager toolManager) {
        toolManager.registerTool(TOOL_NAME, this);
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

            if (requestAndDocumentId.request().isBlank()) {
                return new ToolResponse("Invalid tool usage format.", List.of(), false);
            }

            String base64Query = base64Encoder.encodeToString(requestAndDocumentId.request().getBytes());

            RagQueryRequest ragQueryRequest = new RagQueryRequest(userRole, base64Query);

            String ragRequestString = jsonParser.writeValueAsString(ragQueryRequest);

            HttpRequest httpRequest = HttpRequest
                    .newBuilder(URI.create(ragUrl + QUERY_SUB_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(ragRequestString))
                    .build();

            HttpResponse<String> httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            String ragResponseString = httpResponse.body();

            RagQueryResponse ragQueryResponse = jsonParser.readValue(ragResponseString, RagQueryResponse.class);

            StringBuilder result = new StringBuilder("Rag result:\n");

            if (ragQueryResponse.error()) {
                result.append("Error occurred while retrieving chunks.");
            } else {
                int chunkNumber = 1;
                for (Chunk chunk : ragQueryResponse.chunks()) {
                    result.append(formatChunk(chunk, chunkNumber++));

                    result.append("------------------------");
                }
            }
            return new ToolResponse(result.toString(), List.of(), false);
        } catch (Exception e) {
            return new ToolResponse("Error occurred while using the tool.", List.of(), false);
        }
    }

    private String formatChunk(Chunk chunk, int chunkNumber) {
        return """
                Chunk %d:
                
                %s""".formatted(chunkNumber, new String(base64Decoder.decode(chunk.base64_data)));
    }

    public boolean uploadDocument(UserRole userRole, String documentId, String content) {
        try {
            String base64Content = base64Encoder.encodeToString(content.getBytes(StandardCharsets.UTF_8));

            RagUploadRequest ragUploadRequest = new RagUploadRequest(userRole.toString(), documentId, base64Content);

            String requestString = jsonParser.writeValueAsString(ragUploadRequest);

            HttpRequest httpRequest = HttpRequest
                    .newBuilder(
                            URI.create(ragUrl + UPLOAD_SUB_URL)
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(requestString)
                    )
                    .build();

            HttpResponse<Void> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.discarding()
            );

            return httpResponse.statusCode() == 200;
        } catch (Exception _) {
            return false;
        }
    }

    public boolean deleteDocument(UserRole userRole, String documentId) {
        try {
            RagDeleteRequest ragDeleteRequest = new RagDeleteRequest(userRole.toString(), documentId);

            String deleteRequestString = jsonParser.writeValueAsString(ragDeleteRequest);

            HttpRequest httpRequest = HttpRequest

                    .newBuilder(URI.create(ragUrl + DELETE_SUB_URL))

                    .POST(
                            HttpRequest.BodyPublishers.ofString(deleteRequestString)
                    )

                    .build();

            HttpResponse<Void> httpResponse =
                    httpClient.send(
                            httpRequest,

                            HttpResponse.BodyHandlers.discarding()
                    );

            return httpResponse.statusCode() == 200;
        } catch (Exception _) {
            return false;
        }
    }

    record RagQueryRequest(UserRole user_role, String base64_query) {
    }

    record Chunk(String base64_data) {
    }

    record RagQueryResponse(boolean error, List<Chunk> chunks) {
    }

    record RagUploadRequest(String user_role, String document_id, String base64_content) {
    }

    record RagDeleteRequest(String user_role, String document_id) {
    }
}
