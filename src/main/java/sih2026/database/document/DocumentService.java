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

import org.springframework.stereotype.Service;
import sih2026.database.user.UserRole;

import java.util.Optional;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void addDocumentData(String username, UserRole userRole, String documentName, String documentId, String mimeType) {
        Document document = new Document(username, userRole, documentName, documentId, mimeType);
        documentRepository.save(document);
    }

    public Document getDocumentData(String username, UserRole userRole, String documentId) {
        Optional<Document> document = documentRepository.findByUsernameAndUserRoleAndDocumentId(username, userRole, documentId);
        return document.orElse(null);
    }

    public Document getDocumentDataByDocumentId(String documentId) {
        Optional<Document> document = documentRepository.findByDocumentId(documentId);
        return document.orElse(null);
    }

    public String getDocumentMimeType(String username, UserRole userRole, String documentId) {
        Document document = getDocumentData(username, userRole, documentId);
        if (document == null) {
            return null;
        }
        return document.getMimeType();
    }

    public boolean documentExists(String username, UserRole userRole, String documentId) {
        return documentRepository.existsByUsernameAndUserRoleAndDocumentId(username, userRole, documentId);
    }
}
