package sih2026.web;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;

import java.util.LinkedList;
import java.util.List;

@RestController
public class DocumentUpload {
    private final DocumentStorageManager documentStorageManager;

    public DocumentUpload(DocumentStorageManager documentStorageManager) {
        this.documentStorageManager = documentStorageManager;
    }

    @PostMapping("/upload")
    public Result upload(@RequestParam(value = "files") MultipartFile[] files, Authentication authentication) {
        String username = authentication.getName();

        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        UserRole userRole = UserRole.valueOf(role);

        List<DocumentStorageManager.DocumentNameAndId> documents = new LinkedList<>();

        for (MultipartFile multipartFile : files) {
            DocumentStorageManager.DocumentNameAndId documentNameAndId = documentStorageManager.storeDocument(username, userRole, multipartFile);
            if (documentNameAndId != null) {
                documents.add(documentNameAndId);
            }
        }

        return new Result(documents);
    }

    public record Result(List<DocumentStorageManager.DocumentNameAndId> documents) {
    }
}
