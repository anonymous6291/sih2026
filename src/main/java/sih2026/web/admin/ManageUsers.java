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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sih2026.database.user.UserData;
import sih2026.database.user.UserRole;
import sih2026.database.user.UserService;

@RestController
public class ManageUsers {

    private final UserService userService;

    public ManageUsers(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/admin/register")
    public ResponseEntity<String> registerUser(@RequestBody UserData userData) {
        try {
            userService.addUser(userData);
            return ResponseEntity.ok("User registered.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/admin/deregister/{username}")
    public ResponseEntity<String> deregister(@PathVariable("username") String username) {
        if (userService.removeUserByUsername(username)) {
            return ResponseEntity.ok("User removed.");
        }
        return ResponseEntity.badRequest().build();
    }

    // Remove
    @GetMapping("/register")
    public ResponseEntity<String> register() {
        try {
            UserData request = new UserData(
                    "a1",
                    "123",
                    UserRole.ADMIN,
                    "Anonymous",
                    "01/01/2026",
                    "+911234567890",
                    "xyz@gmail.com",
                    "Admin");

            userService.addUser(request);

            request = new UserData(
                    "a2",
                    "123",
                    UserRole.TECHNICAL_DEPARTMENT,
                    "Anonymous",
                    "01/01/2026",
                    "+911234567890",
                    "xyz@gmail.com",
                    "Admin");
            userService.addUser(request);


            request = new UserData(
                    "a3",
                    "123",
                    UserRole.MECHANICAL_DEPARTMENT,
                    "Anonymous",
                    "01/01/2026",
                    "+911234567890",
                    "xyz@gmail.com",
                    "Admin");
            userService.addUser(request);

            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}