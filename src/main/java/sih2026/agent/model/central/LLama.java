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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedList;
import java.util.List;

public class LLama implements ModelCommunicator {
    private final ObjectMapper jsonParser;

    LLama() {
        jsonParser = new ObjectMapper();
        jsonParser.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jsonParser.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @Override
    public String parseQueryToJsonString(Model.ModelConfig modelConfig, String prompt, Conversation conversation) throws Exception {
        List<Message> messages = new LinkedList<>();

        messages.add(new Message("system", prompt));

        messages.addAll(
                conversation
                        .messages()
                        .stream()
                        .map(
                                message ->
                                        new Message(
                                                message.role(),
                                                message.content()
                                        )
                        ).toList()
        );

        LlmQuery llmQuery = new LlmQuery(modelConfig.model_name(), messages, modelConfig.temperature(), modelConfig.max_tokens(), false);

        IO.println(jsonParser.writeValueAsString(llmQuery));

        return jsonParser.writeValueAsString(llmQuery);
    }

    @Override
    public Response parseResponseAndGetMessage(String response) throws Exception {
        IO.println(response);
        LlmResponse llmResponse = jsonParser.convertValue(jsonParser.readTree(response).findPath("message"), LlmResponse.class);

        return new Response(llmResponse.role(), llmResponse.content());
    }

    record LlmQuery(String model, List<Message> messages, float temperature, int max_tokens, boolean stream) {
    }

    record Message(String role, String content) {
    }

    record LlmResponse(String role, String content) {
    }
}