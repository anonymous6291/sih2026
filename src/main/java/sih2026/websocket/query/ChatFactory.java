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

import org.springframework.stereotype.Service;
import sih2026.database.chat.ChatService;
import sih2026.database.user.UserRole;

@Service
public class ChatFactory {
    private static final int MAX_CHAT_DESCRIPTION = 15;
    private final ChatService chatService;

    public ChatFactory(ChatService chatService) {
        this.chatService = chatService;
    }

    private String getChatDescription(QueryData queryData) {
        String query = queryData.query();
        if (query.length() <= MAX_CHAT_DESCRIPTION) {
            return query;
        }
        return query.substring(0, MAX_CHAT_DESCRIPTION).concat("...");
    }

    public String createNewChatFor(String username, UserRole userRole, QueryData queryData) {
        return chatService.createNewChat(username, userRole, getChatDescription(queryData));
    }
}
