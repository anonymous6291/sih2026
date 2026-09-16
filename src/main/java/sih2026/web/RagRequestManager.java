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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sih2026.agent.RagManager;
import sih2026.database.rag.RagDocumentStorageService;
import sih2026.database.user.UserRole;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/rag")
public class RagRequestManager {
    private final RagManager ragManager;
    private final ObjectMapper jsonParser = new ObjectMapper();

    public RagRequestManager(RagManager ragManager) {
        this.ragManager = ragManager;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(Authentication authentication, @RequestPart("files") MultipartFile[] multipartFiles) {
        try {
            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                    .findFirst()
                    .orElse(null);

            UserRole userRole = UserRole.valueOf(role);

            StringBuilder stringBuilder = new StringBuilder();

            for (MultipartFile multipartFile : multipartFiles) {

                if (!ragManager.storeDocument(userRole, multipartFile)) {
                    String fileName = multipartFile.getOriginalFilename();

                    if (fileName != null) {
                        stringBuilder.append(Path.of(fileName).normalize().getFileName());
                    }
                }

            }

            if (stringBuilder.isEmpty()) {
                return ResponseEntity.ok("Done");
            }

            return ResponseEntity
                    .badRequest()
                    .body("Failed to add files:\n".concat(stringBuilder.toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents")
    public String listAllDocuments(Authentication authentication) {
        try {
            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                    .findFirst()
                    .orElse(null);

            UserRole userRole = UserRole.valueOf(role);

            List<RagDocumentStorageService.DocumentNameIdMimeType> nameIdMimeTypes =
                    ragManager.getStoredDocumentsList(userRole);

            RagDocumentsList ragDocumentsList = new RagDocumentsList(nameIdMimeTypes);

            return jsonParser.writeValueAsString(ragDocumentsList);
        } catch (Exception e) {
            return "Error occurred.";
        }
    }

    @PostMapping("/delete/{documentId}")
    public ResponseEntity<Void> deleteDocumentById(Authentication authentication, @PathVariable("documentId") String documentId) {
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        UserRole userRole = UserRole.valueOf(role);

        if (ragManager.deleteDocument(userRole, documentId)) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadFile(Authentication authentication, @PathVariable("documentId") String documentId) {
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        UserRole userRole = UserRole.valueOf(role);

        RagDocumentStorageService.DocumentNameIdPathMimeType nameIdPathMimeType
                = ragManager.getDocumentData(userRole, documentId);

        if (nameIdPathMimeType == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(nameIdPathMimeType.documentPath());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nameIdPathMimeType.documentName() + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    public record RagDocumentsList(List<RagDocumentStorageService.DocumentNameIdMimeType> documents) {
    }
}
