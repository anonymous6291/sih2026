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

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sih2026.database.user.UserRole;
import sih2026.logging.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class DocumentStorageManager {
    private static final String DOCUMENT_STORAGE_PATH = "documents/";
    private final Logger logger;
    private final DocumentService documentService;
    private final Tika tika = new Tika();

    public DocumentStorageManager(Logger logger, DocumentService documentService) {
        this.logger = logger;
        this.documentService = documentService;
    }

    private String getFileName(String fileName) {
        if (fileName == null) {
            return "no_name";
        }

        return Path.of(fileName).normalize().getFileName().toString();
    }

    private Path getDocumentStoragePath(String username, UserRole userRole, String documentId) {
        return Path.of(DOCUMENT_STORAGE_PATH, userRole.toString(), getFileName(username), getFileName(documentId));
    }

    public DocumentNameAndId storeDocument(String username, UserRole userRole, String documentName, InputStream inputStream) {
        try {
            String documentId = UUID.randomUUID().toString();
            Path filePath = getDocumentStoragePath(username, userRole, documentId);
            Files.createDirectories(filePath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                inputStream.transferTo(outputStream);
            }
            String mimeType = tika.detect(filePath);
            documentService.addDocumentData(username, userRole, documentName, documentId, mimeType);
            return new DocumentNameAndId(documentName, documentId);
        } catch (Exception e) {
            logger.logError("Failed to store document for [" + username + "] [" + userRole + "] : " + e.getMessage());
            return null;
        }
    }

    public DocumentNameAndId storeDocument(String username, UserRole userRole, MultipartFile multipartFile) {
        try {
            String documentId = UUID.randomUUID().toString();
            String documentName = getFileName(multipartFile.getOriginalFilename());
            Path filePath = getDocumentStoragePath(username, userRole, documentId);
            Files.createDirectories(filePath.getParent());
            multipartFile.transferTo(filePath);
            String mimeType = tika.detect(filePath);
            documentService.addDocumentData(username, userRole, documentName, documentId, mimeType);
            return new DocumentNameAndId(documentName, documentId);
        } catch (Exception e) {
            logger.logError("Failed to store document for [" + username + "] [" + userRole + "] : " + e.getMessage());
            return null;
        }
    }

    public DocumentNameAndPath getDocumentNameAndPath(String username, UserRole userRole, String documentId) {
        Document document = documentService.getDocumentData(username, userRole, documentId);
        if (document == null) {
            return null;
        }
        return new DocumentNameAndPath(document.getDocumentName(), getDocumentStoragePath(username, userRole, documentId));
    }

    public DocumentNameAndPath getDocumentNameAndPathByDocumentId(String documentId) {
        Document document = documentService.getDocumentDataByDocumentId(documentId);
        if (document == null) {
            return null;
        }
        return new DocumentNameAndPath(
                document.getDocumentName(),
                getDocumentStoragePath(
                        document.getUsername(),
                        document.getUserRole(),
                        document.getDocumentId()
                )
        );
    }

    public DocumentData getDocumentData(String username, UserRole userRole, String documentId) {
        Document document = documentService.getDocumentData(username, userRole, documentId);
        if (document == null) {
            return null;
        }
        return new DocumentData(
                document.getDocumentName(),
                document.getDocumentId(),
                document.getMimeType(),
                getDocumentStoragePath(username, userRole, documentId)
        );
    }

    public record DocumentNameAndId(String document_name, String document_id) {
    }

    public record DocumentNameAndPath(String documentName, Path documentPath) {
    }

    public record DocumentData(String document_name, String document_id, String mime_type, Path document_path) {
    }
}
