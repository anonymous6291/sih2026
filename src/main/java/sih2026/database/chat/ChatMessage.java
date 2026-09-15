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

import java.util.List;

public record ChatMessage(boolean is_user, String messageId, String message, List<DocumentNameIdPair> documents) {
}
