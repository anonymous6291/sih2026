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

import org.springframework.data.jpa.repository.JpaRepository;
import sih2026.database.user.UserRole;

import java.util.List;
import java.util.Optional;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {
    void deleteByUserRoleAndDocumentId(UserRole userRole, String documentId);

    List<RagDocument> findAllByUserRole(UserRole userRole);

    Optional<RagDocument> findByUserRoleAndDocumentId(UserRole userRole, String documentId);
}
