package sih2026.database.notice;

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

@Getter
@Setter
@Entity
@Table(name = "notice")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = false)
    private String noticeId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private boolean seen;

    Notice() {
    }

    public Notice(String username, UserRole userRole, String noticeId, String description, String message, boolean seen) {
        this.username = username;
        this.userRole = userRole;
        this.noticeId = noticeId;
        this.description = description;
        this.message = message;
        this.time = LocalDateTime.now().toString();
        this.seen = seen;
    }
}
