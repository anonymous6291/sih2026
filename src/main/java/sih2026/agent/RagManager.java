package sih2026.agent;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sih2026.agent.tools.documentparser.DocumentParserTool;
import sih2026.agent.tools.rag.RagTool;
import sih2026.database.rag.RagDocumentStorageService;
import sih2026.database.user.UserRole;

import java.util.List;

@Service
public class RagManager {
    private final RagDocumentStorageService ragDocumentStorageService;
    private final DocumentParserTool documentParserTool;
    private final RagTool ragTool;

    public RagManager(RagDocumentStorageService ragDocumentStorageService, DocumentParserTool documentParserTool, RagTool ragTool) {
        this.ragDocumentStorageService = ragDocumentStorageService;
        this.documentParserTool = documentParserTool;
        this.ragTool = ragTool;
    }

    public boolean storeDocument(UserRole userRole, MultipartFile multipartFile) {
        try {
            RagDocumentStorageService.DocumentNameIdPathMimeType nameIdPathMimeType
                    = ragDocumentStorageService.storeDocument(userRole, multipartFile);

            try {
                String content = documentParserTool.readFile(nameIdPathMimeType.documentPath(), nameIdPathMimeType.mimeType());

                if (
                        ragTool.uploadDocument(
                                userRole,
                                nameIdPathMimeType.documentId(),
                                content
                        )
                ) {
                    return true;
                }

                throw new RuntimeException();
            } catch (Exception e) {
                ragDocumentStorageService.deleteDocument(userRole, nameIdPathMimeType.documentId());
                return false;
            }
        } catch (Exception _) {
            return false;
        }
    }

    public RagDocumentStorageService.DocumentNameIdPathMimeType getDocumentData(UserRole userRole, String documentId) {
        return ragDocumentStorageService.getDocumentWithId(userRole, documentId);
    }

    public List<RagDocumentStorageService.DocumentNameIdMimeType> getStoredDocumentsList(UserRole userRole) {
        return ragDocumentStorageService.getAllDocumentNameAndIdsForUserRole(userRole);
    }

    public boolean deleteDocument(UserRole userRole, String documentId) {
        if (!ragDocumentStorageService.deleteDocument(userRole, documentId)) {
            return false;
        }

        return ragTool.deleteDocument(userRole, documentId);
    }
}
