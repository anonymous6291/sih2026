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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import sih2026.UniversalErrorJSON;
import sih2026.database.user.UserService;

@RestController
public class UserProfile {
    private final UserService userService;
    private final ObjectMapper jsonParser = new ObjectMapper();

    public UserProfile(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/getprofile/{username}")
    public String getUserProfile(@PathVariable("username") String username) {
        try {
            UserService.UserInfo userInfo = userService.getUserDetails(username);
            if (userInfo == null) {
                return UniversalErrorJSON.getErrorJSON("User with username [" + username + "] doesn't exist.");
            }
            return jsonParser.writeValueAsString(userInfo);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON("Unexpected error occurred during json parsing.");
        }
    }
}
