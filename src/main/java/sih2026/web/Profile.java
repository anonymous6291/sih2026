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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sih2026.database.user.UserService;

import java.security.Principal;

@RestController
public class Profile {
    private final UserService userService;

    public Profile(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserService.UserInfo getProfile(Principal principal) {
        String username = principal.getName();
        return userService.getUserDetails(username);
    }
}
