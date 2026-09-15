package sih2026.database.notice;

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
import sih2026.database.user.UserRole;

import java.util.List;
import java.util.Optional;

@Service
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<NoticeInfo> getAllNotices(String username, UserRole userRole) {
        return noticeRepository.findAllByUsernameAndUserRole(username, userRole)
                .stream().map(notice -> new NoticeInfo(
                                notice.getNoticeId(),
                                notice.getDescription(),
                                "",
                                notice.isSeen(),
                                notice.getTime()
                        )
                )
                .toList();
    }

    public List<NoticeInfo> getUnseenNotices(String username, UserRole userRole) {
        return noticeRepository.findAllByUsernameAndUserRoleAndSeen(username, userRole, false)
                .stream().map(notice -> new NoticeInfo(
                                notice.getNoticeId(),
                                notice.getDescription(),
                                "",
                                notice.isSeen(),
                                notice.getTime()
                        )
                )
                .toList();
    }

    public NoticeInfo getNotice(String username, UserRole userRole, String noticeId) {
        Optional<Notice> optionalNotice = noticeRepository.findByUsernameAndUserRoleAndNoticeId(username, userRole, noticeId);
        if (optionalNotice.isEmpty()) {
            return null;
        }
        Notice notice = optionalNotice.get();

        if (!notice.isSeen()) {
            notice.setSeen(true);

            noticeRepository.save(notice);
        }

        return new NoticeInfo(
                notice.getNoticeId(),
                notice.getDescription(),
                notice.getMessage(),
                notice.isSeen(),
                notice.getTime()
        );
    }

    public void issueNotice(String username, UserRole userRole, String noticeId, String description, String message) {
        Notice notice = new Notice(
                username,
                userRole,
                noticeId,
                description,
                message,
                false
        );

        noticeRepository.save(notice);
    }

    public record NoticeInfo(String notice_id, String description, String message, boolean seen, String time) {
    }
}
