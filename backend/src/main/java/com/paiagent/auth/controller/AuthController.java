package com.paiagent.auth.controller;

import com.paiagent.auth.dto.LoginRequest;
import com.paiagent.auth.dto.LoginResponse;
import com.paiagent.auth.dto.RegisterRequest;
import com.paiagent.auth.entity.User;
import com.paiagent.auth.service.AuthService;
import com.paiagent.common.result.R;
import com.paiagent.common.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<LoginResponse> register(@RequestBody @Valid RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        User user = SecurityUtil.getCurrentUser();
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("role", user.getRole());
        return R.ok(info);
    }
}
