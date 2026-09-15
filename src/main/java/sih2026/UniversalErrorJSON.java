package sih2026;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

public class UniversalErrorJSON {
    public static String getErrorJSON(String message) {
        return "{\"status\" : \"error\", \"message\" : \"" + message + "\"}";
    }
}
