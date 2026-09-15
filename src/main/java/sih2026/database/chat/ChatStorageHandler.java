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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import sih2026.database.user.UserRole;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

@Service
public class ChatStorageHandler {
    private static final String CHAT_STORAGE_PATH = "chats/";
    private static final String CHAT_SUMMARY_FILE_EXTENSION = ".summary";
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final ObjectMapper jsonParser = new ObjectMapper();

    private String getFileName(String path) {
        return Path.of(path).normalize().getFileName().toString();
    }

    private Path getChatFilePath(String username, UserRole userRole, String chatId) {
        return Path.of(CHAT_STORAGE_PATH, userRole.toString(), getFileName(username), getFileName(chatId));
    }

    private Path getChatSummaryFilePath(String username, UserRole userRole, String chatId) {
        return Path.of(getChatFilePath(username, userRole, chatId).toString().concat(CHAT_SUMMARY_FILE_EXTENSION));
    }

    public void addChatMessage(String username, UserRole userRole, String chatId, ChatMessage... chatMessages) throws Exception {
        Path chatFilePath = getChatFilePath(username, userRole, chatId);
        Files.createDirectories(chatFilePath.getParent());
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(chatFilePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            for (ChatMessage chatMessage : chatMessages) {
                InternalChatMessageFormat internalChatMessageFormat
                        = new InternalChatMessageFormat(
                        chatMessage.is_user(),
                        chatMessage.messageId(),
                        base64Encoder.encodeToString(chatMessage.message().getBytes(StandardCharsets.UTF_8)),
                        chatMessage.documents()
                );
                String jsonChat = jsonParser.writeValueAsString(internalChatMessageFormat).concat("\n");
                bufferedWriter.write(jsonChat);
            }
        }
    }

    public void setChatSummary(String username, UserRole userRole, String chatId, String summary) throws Exception {
        Path chatSummaryPath = getChatSummaryFilePath(username, userRole, chatId);
        Files.createDirectories(chatSummaryPath.getParent());
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(chatSummaryPath)) {
            bufferedWriter.write(summary);
        }
    }

    public List<ChatMessage> getChatMessages(String username, UserRole userRole, String chatId) throws Exception {
        Path chatFilePath = getChatFilePath(username, userRole, chatId);
        if (!Files.exists(chatFilePath) || !Files.isRegularFile(chatFilePath)) {
            return List.of();
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(chatFilePath)) {
            List<ChatMessage> messages = new LinkedList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                InternalChatMessageFormat internalChatMessageFormat
                        = jsonParser.readValue(line, InternalChatMessageFormat.class);

                messages.add(
                        new ChatMessage(
                                internalChatMessageFormat.is_user(),
                                internalChatMessageFormat.message_id(),
                                new String(
                                        base64Decoder.decode(internalChatMessageFormat.base64_message()),
                                        StandardCharsets.UTF_8
                                ),
                                internalChatMessageFormat.documents()
                        )
                );
            }
            return messages;
        }
    }

    public String getChatSummary(String username, UserRole userRole, String chatId) throws Exception {
        Path chatSummaryFilePath = getChatSummaryFilePath(username, userRole, chatId);
        if (!Files.exists(chatSummaryFilePath) || !Files.isRegularFile(chatSummaryFilePath)) {
            return "";
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(chatSummaryFilePath)) {
            return bufferedReader.readAllAsString();
        }
    }

    public void deleteChat(String username, UserRole userRole, String chatId) throws Exception {
        Path chatFilePath = getChatFilePath(username, userRole, chatId);
        Path chatSummaryFilePath = getChatSummaryFilePath(username, userRole, chatId);
        Files.delete(chatFilePath);
        Files.delete(chatSummaryFilePath);
        Files.delete(chatFilePath.getParent());
    }

    record InternalChatMessageFormat(boolean is_user, String message_id, String base64_message,
                                     List<DocumentNameIdPair> documents) {
    }
}
