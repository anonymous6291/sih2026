package sih2026.database.rag;

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
@Table(name = "rag_document")
public class RagDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = false)
    private String documentName;

    @Column(nullable = false)
    private String documentId;

    @Column(nullable = false)
    private String mimeType;

    RagDocument() {
    }

    public RagDocument(UserRole userRole, String documentName, String documentId, String mimeType) {
        this.userRole = userRole;
        this.documentName = documentName;
        this.documentId = documentId;
        this.mimeType = mimeType;
    }
}
