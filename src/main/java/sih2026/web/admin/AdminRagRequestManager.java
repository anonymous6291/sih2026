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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sih2026.agent.RagManager;
import sih2026.database.rag.RagDocumentStorageService;
import sih2026.database.user.UserRole;
import sih2026.web.RagRequestManager;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/admin/rag")
public class AdminRagRequestManager {
    private final RagManager ragManager;
    private final ObjectMapper jsonParser = new ObjectMapper();

    public AdminRagRequestManager(RagManager ragManager) {
        this.ragManager = ragManager;
    }

    @PostMapping("/upload/{department}")
    public ResponseEntity<String> uploadDocument(
            @PathVariable("department") String role,
            @RequestPart("files") MultipartFile[] multipartFiles
    ) {

        try {
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
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/documents/{department}")
    public String listAllDocuments(@PathVariable("department") String role) {
        try {
            UserRole userRole = UserRole.valueOf(role);

            List<RagDocumentStorageService.DocumentNameIdMimeType> nameIdMimeTypes =
                    ragManager.getStoredDocumentsList(userRole);

            RagRequestManager.RagDocumentsList ragDocumentsList = new RagRequestManager.RagDocumentsList(nameIdMimeTypes);

            return jsonParser.writeValueAsString(ragDocumentsList);
        } catch (Exception e) {
            return "Error occurred.";
        }
    }

    @PostMapping("/delete/{department}/{documentId}")
    public ResponseEntity<Void> deleteDocumentById(@PathVariable("department") String role, @PathVariable("documentId") String documentId) {
        UserRole userRole = UserRole.valueOf(role);

        if (ragManager.deleteDocument(userRole, documentId)) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/download/{department}/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("department") String role, @PathVariable("documentId") String documentId) {
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
}
