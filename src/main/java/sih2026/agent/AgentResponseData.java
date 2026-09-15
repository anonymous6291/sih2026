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

import java.util.List;

public record AgentResponseData(String status, boolean is_event, boolean pending, String message, String chat_id,
                                String message_id,
                                List<DocumentNameIdPair> documents) {
}
