package sih2026.web;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sih2026.UniversalErrorJSON;
import sih2026.database.chat.*;
import sih2026.database.user.UserRole;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/history")
public class ChatHistory {
    private final ChatService chatService;
    private final ChatStorageHandler chatStorageHandler;
    private final Base64.Encoder base64Encoder;
    private final ObjectMapper jsonParser;

    public ChatHistory(ChatService chatService, ChatStorageHandler chatStorageHandler) {
        this.chatService = chatService;
        this.chatStorageHandler = chatStorageHandler;
        base64Encoder = Base64.getEncoder();
        jsonParser = new ObjectMapper();
    }

    @GetMapping("/chats")
    public String getChats(Authentication authentication) {
        String username = authentication.getName();
        try {
            List<Chat> chats = chatService.getAllChats(username);
            List<ChatHistoryMessage> chatHistoryMessages = new LinkedList<>();
            chats.forEach(chat ->
                    chatHistoryMessages.add(new ChatHistoryMessage(chat.getChatId(), chat.getDescription()))
            );
            ChatHistoryFormat chatHistoryFormat = new ChatHistoryFormat(chatHistoryMessages);
            return jsonParser.writeValueAsString(chatHistoryFormat);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON("Failed to retrieve chats.");
        }
    }

    @GetMapping("/chat/{id}")
    public String getChats(@PathVariable(value = "id") String chatId, Authentication authentication) {

        String username = authentication.getName();
        try {
            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                    .findFirst()
                    .orElse(null);

            UserRole userRole = UserRole.valueOf(role);

            List<ChatMessage> chatMessages = chatStorageHandler.getChatMessages(username, userRole, chatId);
            List<Base64EncodedChatMessage> base64EncodedChatMessages = new LinkedList<>();
            chatMessages.forEach(chatMessage ->
                    base64EncodedChatMessages.add(
                            new Base64EncodedChatMessage(
                                    chatMessage.is_user(),
                                    chatMessage.messageId(),
                                    base64Encoder.encodeToString(chatMessage.message().getBytes(StandardCharsets.UTF_8)),
                                    chatMessage.documents()
                            )
                    ));
            Base64EncodedChatMessages base64EncodedChat = new Base64EncodedChatMessages(base64EncodedChatMessages);
            return jsonParser.writeValueAsString(base64EncodedChat);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON("Failed to retrieve chat.");
        }
    }

    public record ChatHistoryMessage(String chat_id, String description) {
    }

    public record ChatHistoryFormat(List<ChatHistoryMessage> chats) {
    }

    public record Base64EncodedChatMessages(List<Base64EncodedChatMessage> messages) {
    }

    public record Base64EncodedChatMessage(boolean is_user, String message_id, String base64_message,
                                           List<DocumentNameIdPair> documents) {
    }
}
