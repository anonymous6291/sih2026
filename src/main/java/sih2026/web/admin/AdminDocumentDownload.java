package sih2026.web.admin;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sih2026.database.document.DocumentStorageManager;

@Controller
public class AdminDocumentDownload {
    private final DocumentStorageManager documentStorageManager;

    public AdminDocumentDownload(DocumentStorageManager documentStorageManager) {
        this.documentStorageManager = documentStorageManager;
    }

    @GetMapping("/admin/download/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("documentId") String documentId) {

        if (documentId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        DocumentStorageManager.DocumentNameAndPath documentNameAndPath =
                documentStorageManager.getDocumentNameAndPathByDocumentId(documentId);

        if (documentNameAndPath == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(documentNameAndPath.documentPath());

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
