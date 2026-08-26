package org.civicfix.app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.CreateUserRequest;
import org.civicfix.app.dto.UserResponse;
import org.civicfix.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{userId}/team/{teamId}")
    public ResponseEntity<UserResponse> assignTeam(@PathVariable Long userId, @PathVariable Long teamId) {
        return ResponseEntity.ok(userService.assignTeam(userId, teamId));
    }
}
