package sih2026.agent;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import sih2026.database.chat.DocumentNameIdPair;
import sih2026.database.user.UserRole;

import java.util.List;

public record AgentRequestData(String query, String username, UserRole userRole, String chat_id, String message_id,
                               String model_id, List<DocumentNameIdPair> documents, List<String> tools) {
}