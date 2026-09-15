package sih2026.websocket.query;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import sih2026.UniversalErrorJSON;
import sih2026.agent.AgentResponseData;
import sih2026.logging.Logger;
import sih2026.websocket.event.UserEventSender;

import java.util.function.Consumer;

public class AgentEventHandler implements Consumer<AgentResponseData> {
    private final Logger logger;
    private final UserEventSender userEventSender;
    private final String username;
    private final ObjectMapper jsonParser;

    public AgentEventHandler(Logger logger, UserEventSender userEventSender, String username) {
        this.logger = logger;
        this.userEventSender = userEventSender;
        this.username = username;
        jsonParser = new ObjectMapper();
    }

    private String getErrorAgentResponseString() {
        return UniversalErrorJSON.getErrorJSON("Unexpected error occurred.");
    }

    @Override
    public void accept(AgentResponseData agentResponseData) {
        if (agentResponseData == null) {
            userEventSender.sendEventToUser(username, getErrorAgentResponseString());
            return;
        }
        try {
            String data = jsonParser.writeValueAsString(agentResponseData);
            userEventSender.sendEventToUser(username, data);
        } catch (Exception e) {
            logger.logWarn("Failed to send event to [" + username + "] : " + e.getMessage());
            userEventSender.sendEventToUser(username, getErrorAgentResponseString());
        }
    }
}
