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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import sih2026.database.user.UserRole;

@Controller
public class Home {
    @GetMapping({"/", "/home"})
    public String getHomePage(Authentication authentication) {
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse(null);

        String username = authentication.getName();
        UserRole userRole = UserRole.valueOf(role);

        if (userRole == UserRole.ADMIN) {
            return "admin_home.html";
        }
        return "home.html";
    }
}