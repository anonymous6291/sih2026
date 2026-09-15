package sih2026.database.document;

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

@Getter
@Setter
@Entity
@Table(name = "document_table")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column
    private String documentName;

    @Column(nullable = false)
    private String documentId;

    @Column(nullable = false)
    private String mimeType;

    public Document() {
    }

    public Document(String username, UserRole userRole, String documentName, String documentId, String mimeType) {
        this.username = username;
        this.userRole = userRole;
        this.documentName = documentName;
        this.documentId = documentId;
        this.mimeType = mimeType;
    }
}
