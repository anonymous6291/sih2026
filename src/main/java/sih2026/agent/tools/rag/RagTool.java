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
import java.time.Duration;
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
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper jsonParser = new ObjectMapper();
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
    public ToolResponse performTask(String username, UserRole userRole, String request) throws Exception {
        try {
            RequestAndDocumentId requestAndDocumentId = parseRequest(request);

            if (requestAndDocumentId.request().isBlank()) {
                return new ToolResponse("Invalid tool usage format.", List.of(), false);
            }

            RagRequest ragRequest = new RagRequest(username, userRole, requestAndDocumentId.request());

            String ragRequestString = jsonParser.writeValueAsString(ragRequest);

            HttpRequest httpRequest = HttpRequest
                    .newBuilder(URI.create(ragUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(ragRequestString))
                    .build();

            HttpResponse<String> httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            String ragResponseString = httpResponse.body();

            RagResponse ragResponse = jsonParser.readValue(ragResponseString, RagResponse.class);

            StringBuilder result = new StringBuilder("Rag result:\n");

            if (ragResponse.error()) {
                result.append("Error occurred while retrieving chunks.");
            } else {
                int chunkNumber = 1;
                for (Chunk chunk : ragResponse.chunks()) {
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
                
                %s""".formatted(chunkNumber, chunk.data);
    }

    record RagRequest(String username, UserRole user_role, String query) {
    }

    record Chunk(String data) {
    }

    record RagResponse(boolean error, List<Chunk> chunks) {
    }
}
