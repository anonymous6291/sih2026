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
import org.springframework.web.multipart.MultipartFile;
import sih2026.database.user.UserRole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class RagDocumentStorageService {
    private static final String STORAGE_PATH = "rag/";
    private final RagDocumentService ragDocumentService;

    public RagDocumentStorageService(RagDocumentService ragDocumentService) {
        this.ragDocumentService = ragDocumentService;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "no_name";
        }

        return Path.of(fileName).normalize().getFileName().toString();
    }

    private Path getDocumentStoragePath(UserRole userRole, String documentId) {
        return Path.of(STORAGE_PATH, userRole.toString(), sanitizeFileName(documentId));
    }

    public DocumentNameIdPathMimeType storeDocument(UserRole userRole, MultipartFile multipartFile) {
        try {
            String documentId = UUID.randomUUID().toString();

            Path filePath = getDocumentStoragePath(userRole, documentId);

            String fileName = sanitizeFileName(multipartFile.getOriginalFilename());

            multipartFile.transferTo(filePath);

            String mimeType = Files.probeContentType(filePath);

            ragDocumentService.addDocument(userRole, documentId, fileName, mimeType);

            return new DocumentNameIdPathMimeType(fileName, documentId, filePath, mimeType);
        } catch (Exception e) {
            return null;
        }
    }

    public List<DocumentNameIdMimeType> getAllDocumentNameAndIdsForUserRole(UserRole userRole) {
        List<RagDocument> ragDocuments = ragDocumentService.getAllDocumentsForUserRole(userRole);

        return ragDocuments
                .stream()
                .map(
                        ragDocument ->
                                new DocumentNameIdMimeType(
                                        ragDocument.getDocumentName(),
                                        ragDocument.getDocumentId(),
                                        ragDocument.getMimeType()
                                )
                )
                .toList();
    }

    public DocumentNameIdPathMimeType getDocumentWithId(UserRole userRole, String documentId) {
        RagDocument ragDocument = ragDocumentService.getDocument(userRole, documentId);

        if (ragDocument == null) {
            return null;
        }

        return new DocumentNameIdPathMimeType(
                ragDocument.getDocumentName(),
                ragDocument.getDocumentId(),
                getDocumentStoragePath(userRole, documentId),
                ragDocument.getMimeType()
        );
    }

    public boolean deleteDocument(UserRole userRole, String documentId) {
        try {
            ragDocumentService.removeDocument(userRole, documentId);

            Path filePath = getDocumentStoragePath(userRole, sanitizeFileName(documentId));

            Files.delete(filePath);
            return true;
        } catch (Exception _) {
            return false;
        }
    }

    public record DocumentNameIdPathMimeType(String documentName, String documentId, Path documentPath,
                                             String mimeType) {
    }

    public record DocumentNameIdMimeType(String document_name, String document_id, String mime_type) {
    }
}
