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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sih2026.UniversalErrorJSON;
import sih2026.database.user.UserRole;
import sih2026.database.user.UserService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class DepartmentInfo {
    private final UserService userService;
    private final ObjectMapper jsonParser = new ObjectMapper();

    public DepartmentInfo(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/departments")
    public String getDepartments() {
        try {
            List<String> departmentsList = Arrays.stream(UserRole.values()).map(UserRole::toString).toList();
            Departments departments = new Departments(departmentsList);
            return jsonParser.writeValueAsString(departments);
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON(e.getMessage());
        }
    }

    @GetMapping("/department/{departmentName}/users")
    public String getAllUsers(@PathVariable("departmentName") String departmentName) {
        try {
            UserRole userRole = UserRole.valueOf(departmentName);

            List<UserService.UserInfo> userInfos = userService.getAllUsersOfDepartment(userRole);

            DepartmentUsers departmentUsers = new DepartmentUsers(userInfos);

            return jsonParser.writeValueAsString(departmentUsers);
        } catch (IllegalArgumentException _) {
            return UniversalErrorJSON.getErrorJSON("Invalid department name.");
        } catch (Exception e) {
            return UniversalErrorJSON.getErrorJSON(e.getMessage());
        }
    }

    public record Departments(List<String> departments) {
    }

    public record DepartmentUsers(List<UserService.UserInfo> users) {
    }
}
