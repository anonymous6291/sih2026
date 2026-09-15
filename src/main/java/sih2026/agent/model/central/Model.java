package sih2026.agent.model.central;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class Model {
    @Getter
    private final ModelConfig modelConfig;
    private final HttpClient httpClient;
    private final ModelCommunicator modelCommunicator;
    private final AtomicBoolean isShutdown;

    public Model(ModelConfig modelConfig) throws Exception {
        this.modelConfig = modelConfig;
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(1)).build();
        Class<?> clazz = Class.forName(modelConfig.handler_class());
        modelCommunicator = (ModelCommunicator) clazz.getDeclaredConstructor().newInstance();
        isShutdown = new AtomicBoolean(false);
    }

    public String getModelName() {
        return modelConfig.model_name();
    }

    public ModelCommunicator.Response sendQuery(String prompt, Conversation conversation) throws Exception {
        if (isShutdown()) {
            throw new IllegalStateException("Model is not running.");
        }
        String query = modelCommunicator.parseQueryToJsonString(modelConfig, prompt, conversation);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(modelConfig.model_url())).POST(HttpRequest.BodyPublishers.ofString(query)).build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String rawResponse = httpResponse.body();
        return modelCommunicator.parseResponseAndGetMessage(rawResponse);
    }

    public boolean isShutdown() {
        return isShutdown.get();
    }

    public void shutdown() {
        httpClient.close();
        isShutdown.set(true);
    }

    public record ModelConfig(String model_id, String model_name, String model_description, boolean is_default,
                              String model_url,
                              int max_tokens, float temperature,
                              String handler_class) {
    }
}