package sih2026.web.user;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;

import java.nio.file.Path;

@RestController
public class UserDocumentDownload {
    private final DocumentStorageManager documentStorageManager;

    public UserDocumentDownload(DocumentStorageManager documentStorageManager) {
        this.documentStorageManager = documentStorageManager;
    }

    @GetMapping("/user/download/{document_id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable(name = "document_id") String documentId, Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        UserRole userRole = UserRole.valueOf(role);

        DocumentStorageManager.DocumentNameAndPath documentNameAndPath = documentStorageManager.getDocumentNameAndPath(username, userRole, documentId);

        if (documentNameAndPath == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = documentNameAndPath.documentPath();

        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + documentNameAndPath.documentName() + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
