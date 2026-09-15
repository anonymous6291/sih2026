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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sih2026.database.user.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String username;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = false)
    private String chatId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime creationTime = LocalDateTime.now();

    public Chat() {
    }

    public Chat(String username, UserRole userRole, String description) {
        this.username = username;
        this.userRole = userRole;
        this.chatId = UUID.randomUUID().toString();
        this.description = description;
    }
}
