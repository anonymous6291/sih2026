package sih2026.web.admin;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sih2026.UniversalErrorJSON;
import sih2026.database.chat.Chat;
import sih2026.database.chat.ChatMessage;
import sih2026.database.chat.ChatService;
import sih2026.database.chat.ChatStorageHandler;
import sih2026.database.user.UserRole;
import sih2026.database.user.UserService;
import sih2026.web.ChatHistory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/admin/history/")
public class UserSpecificChatHistory {
    private final ChatService chatService;
    private final UserService userService;
    private final ChatStorageHandler chatStorageHandler;
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final ObjectMapper jsonParser = new ObjectMapper();

    public UserSpecificChatHistory(ChatService chatService, UserService userService, ChatStorageHandler chatStorageHandler) {
        this.chatService = chatService;
        this.userService = userService;
        this.chatStorageHandler = chatStorageHandler;
    }

    @GetMapping("/{username}/chats")
    public String getChats(@PathVariable("username") String username) {
        try {
            List<Chat> chats = chatService.getAllChats(username);
            List<ChatHistory.ChatHistoryMessage> chatHistoryMessages = new LinkedList<>();
            chats.forEach(chat ->
                    chatHistoryMessages.add(new ChatHistory.ChatHistoryMessage(chat.getChatId(), chat.getDescription()))
            );
            ChatHistory.ChatHistoryFormat chatHistoryFormat = new ChatHistory.ChatHistoryFormat(chatHistoryMessages);
            return jsonParser.writeValueAsString(chatHistoryFormat);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON("Failed to retrieve chats.");
        }
    }

    @GetMapping("/{username}/chat/{chatId}")
    public String getChatMessages(@PathVariable("username") String username, @PathVariable("chatId") String chatId) {
        try {
            UserService.UserInfo user = userService.getUserDetails(username);

            if (user == null) {
                return UniversalErrorJSON.getErrorJSON("Failed to retrieve chat.");
            }

            UserRole userRole = user.role();

            List<ChatMessage> chatMessages = chatStorageHandler.getChatMessages(username, userRole, chatId);

            List<ChatHistory.Base64EncodedChatMessage> base64EncodedChatMessages = new LinkedList<>();

            chatMessages.forEach(chatMessage ->
                    base64EncodedChatMessages.add(
                            new ChatHistory.Base64EncodedChatMessage(
                                    chatMessage.is_user(),
                                    chatMessage.messageId(),
                                    base64Encoder.encodeToString(chatMessage.message().getBytes(StandardCharsets.UTF_8)),
                                    chatMessage.documents()
                            )
                    ));
            ChatHistory.Base64EncodedChatMessages base64EncodedChat = new ChatHistory.Base64EncodedChatMessages(base64EncodedChatMessages);
            return jsonParser.writeValueAsString(base64EncodedChat);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON("Failed to retrieve chat.");
        }
    }
}
