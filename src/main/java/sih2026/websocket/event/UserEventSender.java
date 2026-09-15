package sih2026.websocket.event;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import sih2026.database.user.UserRole;

@Service
public class UserEventSender {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public UserEventSender(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sentNoticeToDepartment(UserRole userRole, String notice) {
        simpMessagingTemplate.convertAndSend("/topic/notice/" + userRole.toString(), notice);
    }

    public void sendEventToUser(String username, String data) {
        simpMessagingTemplate.convertAndSendToUser(username, "/event", data);
    }
}
