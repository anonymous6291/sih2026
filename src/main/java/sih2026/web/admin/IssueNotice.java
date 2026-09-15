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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sih2026.database.notice.NoticeService;
import sih2026.database.user.UserRole;
import sih2026.database.user.UserService;
import sih2026.websocket.event.UserEventSender;

import java.util.List;
import java.util.UUID;

@RestController
public class IssueNotice {
    private final UserService userService;
    private final NoticeService noticeService;
    private final UserEventSender userEventSender;
    private final ObjectMapper jsonParser = new ObjectMapper();

    public IssueNotice(UserService userService, NoticeService noticeService, UserEventSender userEventSender) {
        this.userService = userService;
        this.noticeService = noticeService;
        this.userEventSender = userEventSender;
    }

    @PostMapping("/admin/issuenotice/{department}")
    public ResponseEntity<String> issueNotice(@PathVariable("department") String department, @RequestBody NoticeData noticeData) {
        try {
            String noticeId = UUID.randomUUID().toString();
            if (department.equals("all")) {
                for (UserRole userRole : UserRole.values()) {
                    issueNoticeToDepartment(userRole, noticeId, noticeData);
                }
            } else {
                UserRole userRole = UserRole.valueOf(department);
                issueNoticeToDepartment(userRole, noticeId, noticeData);
            }
        } catch (IllegalArgumentException _) {
            return ResponseEntity.badRequest().body("Department doesn't exist.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        return ResponseEntity.ok("Sent");
    }

    private void issueNoticeToDepartment(UserRole userRole, String noticeId, NoticeData noticeData) throws Exception {
        List<UserService.UserInfo> userInfos = userService.getAllUsersOfDepartment(userRole);
        userInfos.forEach(userInfo ->
                noticeService.issueNotice(
                        userInfo.username(),
                        userRole,
                        noticeId,
                        noticeData.description(),
                        noticeData.message()
                )
        );
        NoticeIssueFormat noticeIssueFormat = new NoticeIssueFormat(noticeId, noticeData.description());
        String noticeIssueFormatString = jsonParser.writeValueAsString(noticeIssueFormat);
        userEventSender.sentNoticeToDepartment(userRole, noticeIssueFormatString);
    }

    // Remove
    @GetMapping("/send/{xyz}")
    public ResponseEntity<String> sendNotice(@PathVariable("xyz") String xyz) {
        try {
            NoticeData noticeData = new NoticeData("Hello from admin.", "Welcome user!!!!");
            String noticeId = UUID.randomUUID().toString();
            if (xyz.equals("all")) {
                for (UserRole userRole : UserRole.values()) {
                    issueNoticeToDepartment(userRole, noticeId, noticeData);
                }
            } else {
                UserRole userRole = UserRole.valueOf(xyz);
                issueNoticeToDepartment(userRole, noticeId, noticeData);
            }
        } catch (IllegalArgumentException _) {
            return ResponseEntity.badRequest().body("Department doesn't exist.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        return ResponseEntity.ok("Sent");
    }

    public record NoticeData(String description, String message) {
    }

    public record NoticeIssueFormat(String notice_id, String description) {
    }
}
