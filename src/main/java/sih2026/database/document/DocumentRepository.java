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

import org.springframework.data.jpa.repository.JpaRepository;
import sih2026.database.user.UserRole;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByUsernameAndUserRoleAndDocumentId(String username, UserRole userRole, String documentId);

    Optional<Document> findByDocumentId(String documentId);

    boolean existsByUsernameAndUserRoleAndDocumentId(String username, UserRole userRole, String documentId);
}
