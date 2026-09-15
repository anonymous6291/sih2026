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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import sih2026.UniversalErrorJSON;
import sih2026.database.notice.NoticeService;
import sih2026.database.user.UserRole;

import java.util.List;

@RestController
public class NoticeFetcher {
    private final NoticeService noticeService;
    private final ObjectMapper jsonParser;

    public NoticeFetcher(NoticeService noticeService) {
        this.noticeService = noticeService;
        jsonParser = new ObjectMapper();
    }

    @GetMapping("/notices/unseen")
    public Notices getUnseenNotices(Authentication authentication) {
        String username = authentication.getName();

        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        UserRole userRole = UserRole.valueOf(role);

        List<NoticeService.NoticeInfo> noticeInfos =
                noticeService.getUnseenNotices(username, userRole);

        return new Notices(noticeInfos);
    }

    @GetMapping("/notices/all")
    public Notices getNotices(Authentication authentication) {
        String username = authentication.getName();

        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        UserRole userRole = UserRole.valueOf(role);

        List<NoticeService.NoticeInfo> noticeInfos =
                noticeService.getAllNotices(username, userRole);

        return new Notices(noticeInfos);
    }

    @GetMapping("/notice/{notice_id}")
    public String getNotice(@PathVariable("noticeId") String noticeId, Authentication authentication) {
        try {
            String username = authentication.getName();

            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                    .findFirst()
                    .orElse(null);

            UserRole userRole = UserRole.valueOf(role);

            NoticeService.NoticeInfo noticeInfo = noticeService.getNotice(username, userRole, noticeId);

            if (noticeInfo == null) {
                return UniversalErrorJSON.getErrorJSON("Notice doesn't exists.");
            }

            return jsonParser.writeValueAsString(noticeInfo);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON("Unknown error occurred.");
        }
    }

    public record Notices(List<NoticeService.NoticeInfo> notices) {
    }
}
