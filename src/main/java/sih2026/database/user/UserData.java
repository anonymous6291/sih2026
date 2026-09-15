package sih2026.database.user;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

public record UserData(
        String username,
        String password,
        UserRole role,
        String name,
        String dob,
        String phoneNumber,
        String email,
        String position
) {
}
