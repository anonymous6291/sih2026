package sih2026.config;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import sih2026.database.user.UserRole;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages
    ) {

        messages
                // Every normal WebSocket message must be authenticated
                .nullDestMatcher().authenticated()

                .simpDestMatchers("/app/admin/**").hasRole(UserRole.ADMIN.toString())

                .simpDestMatchers("/app/user/**").authenticated()

                .simpSubscribeDestMatchers("/user/**").authenticated()

                .simpSubscribeDestMatchers("/topic/notice/" + UserRole.ADMIN)
                .hasRole(UserRole.ADMIN.toString())

                .simpSubscribeDestMatchers("/topic/notice/" + UserRole.TECHNICAL_DEPARTMENT)
                .hasRole(UserRole.TECHNICAL_DEPARTMENT.toString())

                .simpSubscribeDestMatchers("/topic/notice/" + UserRole.MECHANICAL_DEPARTMENT)
                .hasRole(UserRole.MECHANICAL_DEPARTMENT.toString())

                // USER + ADMIN can send to /app/user/**
                //.simpDestMatchers("/app/user/**")
                //.hasRole("USER")

                // ADMIN only
                //.simpDestMatchers("/app/admin/**")
                //.hasRole("ADMIN")

                // USER + ADMIN can subscribe to user topics
                //.simpSubscribeDestMatchers("/topic/user/**")
                //.hasAnyRole("USER", "ADMIN")

                // ADMIN-only subscription
                //.simpSubscribeDestMatchers("/topic/admin/**")
                //.hasRole("ADMIN")

                // Everything else denied
                .anyMessage().denyAll();

        return messages.build();
    }
}