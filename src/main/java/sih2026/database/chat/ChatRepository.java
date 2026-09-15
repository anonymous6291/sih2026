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

import org.springframework.data.jpa.repository.JpaRepository;
import sih2026.database.user.UserRole;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByUsername(String username);

    boolean existsByUsernameAndUserRoleAndChatId(String username, UserRole userRole, String chatId);
}
