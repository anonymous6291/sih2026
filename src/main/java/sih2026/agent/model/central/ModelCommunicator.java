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

public interface ModelCommunicator {
    String parseQueryToJsonString(Model.ModelConfig modelConfig, String prompt, Conversation conversation) throws Exception;

    Response parseResponseAndGetMessage(String rawResponse) throws Exception;

    record Response(String role, String content) {
    }
}
