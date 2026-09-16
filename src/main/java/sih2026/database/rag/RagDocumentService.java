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

import org.springframework.stereotype.Service;
import sih2026.database.user.UserRole;

import java.util.List;

@Service
public class RagDocumentService {
    private final RagDocumentRepository ragDocumentRepository;

    public RagDocumentService(RagDocumentRepository ragDocumentRepository) {
        this.ragDocumentRepository = ragDocumentRepository;
    }

    public void addDocument(UserRole userRole, String documentName, String documentId, String mimeType) {
        RagDocument ragDocument = new RagDocument(userRole, documentName, documentId, mimeType);
        ragDocumentRepository.save(ragDocument);
    }

    public void removeDocument(UserRole userRole, String documentId) {
        ragDocumentRepository.deleteByUserRoleAndDocumentId(userRole, documentId);
    }

    public RagDocument getDocument(UserRole userRole, String documentId) {
        return ragDocumentRepository.findByUserRoleAndDocumentId(userRole, documentId).orElse(null);
    }

    public List<RagDocument> getAllDocumentsForUserRole(UserRole userRole) {
        return ragDocumentRepository.findAllByUserRole(userRole);
    }
}
