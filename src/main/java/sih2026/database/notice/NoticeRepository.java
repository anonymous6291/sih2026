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

import org.springframework.data.jpa.repository.JpaRepository;
import sih2026.database.user.UserRole;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByUsernameAndUserRole(String username, UserRole userRole);

    List<Notice> findAllByUsernameAndUserRoleAndSeen(String username, UserRole userRole, boolean seen);

    Optional<Notice> findByUsernameAndUserRoleAndNoticeId(String username, UserRole userRole, String noticeId);
}