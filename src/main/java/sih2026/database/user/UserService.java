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

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public void addUser(UserData userData) {
        if (userRepository.existsByUsername(userData.username())) {
            throw new IllegalArgumentException("Username already exists.");
        }
        User user = new User(
                userData.username(),
                passwordEncoder.encode(userData.password()),
                userData.role(),
                userData.name(),
                userData.dob(),
                userData.phoneNumber(),
                userData.email(),
                userData.position()
        );
        userRepository.save(user);
    }

    public boolean removeUserByUsername(String username) {
        return userRepository.deleteByUsername(username) > 0;
    }

    public UserInfo getUserDetails(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return null;
        }
        return new UserInfo(
                user.getUsername(),
                user.getRole(),
                user.getName(),
                user.getDob(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getPosition()
        );
    }

    public List<UserInfo> getAllUsersOfDepartment(UserRole userRole) {
        List<User> users = userRepository.findAllByRole(userRole);
        return users.stream().map(user ->
                new UserInfo(
                        user.getUsername(),
                        user.getRole(),
                        user.getName(),
                        user.getDob(),
                        user.getPhoneNumber(),
                        user.getEmail(),
                        user.getPosition()
                )
        ).toList();
    }

    public record UserInfo(
            String username,
            UserRole role,
            String name,
            String dob,
            String phone_number,
            String email,
            String position) {
    }
}
