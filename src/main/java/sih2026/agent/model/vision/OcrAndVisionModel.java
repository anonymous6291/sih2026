package sih2026.agent.model.vision;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

@Service
public class OcrAndVisionModel {
    private static final String CONFIG_FILE = "vision_ocr_model.json";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String modelName;
    private final float temperature;
    private final int maxTokens;

    public OcrAndVisionModel() throws Exception {
        try (BufferedReader bufferedReader = Files.newBufferedReader(Path.of(CONFIG_FILE))) {
            this.objectMapper = new ObjectMapper();

            String configString = bufferedReader.readAllAsString();

            Config config = objectMapper.readValue(configString, Config.class);

            modelName = config.model_name();
            temperature = config.temperature();
            maxTokens = config.max_tokens();

            this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            this.endpoint = URI.create(config.model_url());
        }
    }

    /**
     * Send a prompt together with an image.
     */
    public String sendMessage(
            String prompt,
            InputStream imageInputStream,
            String mimeType
    ) throws IOException, InterruptedException {

        byte[] imageBytes = imageInputStream.readAllBytes();

        String base64 = Base64.getEncoder()
                .encodeToString(imageBytes);

        String dataUri =
                "data:" + mimeType + ";base64," + base64;

        /*
         * {
         *   "role": "user",
         *   "content": [
         *      {
         *          "type": "text",
         *          "text": "..."
         *      },
         *      {
         *          "type": "image_url",
         *          "image_url": {
         *              "url": "data:image/png;base64,..."
         *          }
         *      }
         *   ]
         * }
         */

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);

        ArrayNode messages = root.putArray("messages");

        ObjectNode message = messages.addObject();

        message.put("role", "user");

        ArrayNode content = message.putArray("content");

        // Text
        ObjectNode textPart = content.addObject();

        textPart.put("type", "text");
        textPart.put("text", prompt);

        // Image
        ObjectNode imagePart = content.addObject();

        imagePart.put("type", "image_url");

        ObjectNode imageUrl =
                imagePart.putObject("image_url");

        imageUrl.put("url", dataUri);


        root.put("temperature", temperature);

        root.put("max_tokens", maxTokens);

        root.put("stream", false);

        return send(root);
    }

    /**
     * Send the generated JSON request to llama.cpp.
     */
    private String send(ObjectNode requestBody)
            throws IOException, InterruptedException {

        String json = objectMapper.writeValueAsString(
                requestBody
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(json)
                )
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {

            throw new IOException(
                    "llama.cpp returned HTTP "
                            + response.statusCode()
                            + ":\n"
                            + response.body()
            );
        }

        return extractResponse(response.body());
    }

    /**
     * Extract choices[0].message.content.
     */
    private String extractResponse(String response)
            throws IOException {

        JsonNode root =
                objectMapper.readTree(response);

        JsonNode error = root.get("error");

        if (error != null && !error.isNull()) {
            throw new IOException(
                    "llama.cpp error: " + error
            );
        }

        JsonNode content = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (content.isMissingNode() ||
                content.isNull()) {

            throw new IOException(
                    "Invalid llama.cpp response:\n"
                            + response
            );
        }

        return content.asText();
    }

    /**
     * Determine MIME type of the image.
     */
    /*
    private static String detectMimeType(Path image)
            throws IOException {

        String mimeType =
                Files.probeContentType(image);

        if (mimeType == null) {
            throw new IOException(
                    "Could not determine MIME type: "
                            + image
            );
        }

        if (!mimeType.startsWith("image/")) {
            throw new IOException(
                    "File is not an image: "
                            + mimeType
            );
        }

        return mimeType;
    }
    */

    public record Config(String model_name, String model_url, float temperature, int max_tokens) {
    }
}