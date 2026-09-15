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

import sih2026.database.chat.DocumentNameIdPair;

import java.util.List;

public record QueryData(String query, String chat_id, String message_id, String model_id,
                        List<DocumentNameIdPair> documents,
                        List<String> tools) {
}
