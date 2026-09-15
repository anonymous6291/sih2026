package sih2026.database.chat;

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
import sih2026.database.user.UserRole;

import java.util.List;

@Service
public class ChatService {
    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public List<Chat> getAllChats(String username) {
        return chatRepository.findAllByUsername(username);
    }

    public String createNewChat(String username, UserRole userRole, String description) {
        Chat chat = new Chat(username, userRole, description);
        chatRepository.save(chat);
        return chat.getChatId();
    }

    public boolean chatExists(String username, UserRole userRole, String chatId) {
        return chatRepository.existsByUsernameAndUserRoleAndChatId(username, userRole, chatId);
    }
}
