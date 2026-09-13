package com.opsagent.controller;

import com.opsagent.dto.ApiResponse;
import com.opsagent.dto.AuthRequest;
import com.opsagent.dto.AuthResponse;
import com.opsagent.entity.User;
import com.opsagent.service.UserService;
import com.opsagent.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final UserService userService;

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        Optional<User> userOpt = userService.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return Mono.just(ApiResponse.error(401, "User not found"));
        }
        User user = userOpt.get();
        if (!userService.checkPassword(request.getPassword(), user.getPassword())) {
            return Mono.just(ApiResponse.error(401, "Invalid username or password"));
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return Mono.just(ApiResponse.success(new AuthResponse(token, user.getUsername(), user.getRole())));
    }
}