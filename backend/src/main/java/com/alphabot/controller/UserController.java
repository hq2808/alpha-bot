package com.alphabot.controller;

import com.alphabot.entity.User;
import com.alphabot.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticatedUser User user) {
        return ResponseEntity.ok(user);
    }
}
