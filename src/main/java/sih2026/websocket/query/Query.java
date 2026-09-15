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

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import sih2026.UniversalErrorJSON;
import sih2026.agent.Agent;
import sih2026.agent.AgentRequestData;
import sih2026.database.chat.ChatService;
import sih2026.database.chat.DocumentNameIdPair;
import sih2026.database.document.DocumentService;
import sih2026.database.user.UserRole;
import sih2026.logging.Logger;
import sih2026.websocket.event.UserEventSender;

@Controller
public class Query {
    private final Logger logger;
    private final DocumentService documentService;
    private final ChatService chatService;
    private final ChatFactory chatFactory;
    private final UserEventSender userEventSender;
    private final Agent agent;

    public Query(Logger logger, DocumentService documentService, ChatService chatService, ChatFactory chatFactory, UserEventSender userEventSender, Agent agent) {
        this.logger = logger;
        this.documentService = documentService;
        this.chatService = chatService;
        this.chatFactory = chatFactory;
        this.userEventSender = userEventSender;
        this.agent = agent;
    }

    @MessageMapping({"/user/query", "/admin/query"})
    public void sendQuery(QueryData queryData, Authentication authentication) {
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        String username = authentication.getName();
        UserRole userRole = UserRole.valueOf(role);

        String chatId;

        if (queryData.chat_id() == null || queryData.chat_id().isBlank()) {
            chatId = chatFactory.createNewChatFor(username, userRole, queryData);
        } else {
            chatId = queryData.chat_id();
            if (!chatService.chatExists(username, userRole, chatId)) {
                userEventSender.sendEventToUser(username, UniversalErrorJSON.getErrorJSON("Invalid query."));
                return;
            }
        }

        for (DocumentNameIdPair nameIdPair : queryData.documents()) {
            String documentId = nameIdPair.document_id();
            if (!documentService.documentExists(username, userRole, documentId)) {
                userEventSender.sendEventToUser(username, UniversalErrorJSON.getErrorJSON("Invalid query."));
                return;
            }
        }

        Thread.startVirtualThread(() ->
                agent.process(
                        new AgentRequestData(
                                queryData.query(),
                                authentication.getName(),
                                userRole,
                                chatId,
                                queryData.message_id(),
                                queryData.model_id(),
                                queryData.documents(),
                                queryData.tools()
                        ),
                        new AgentEventHandler(logger, userEventSender, username)
                )
        );

    }
}