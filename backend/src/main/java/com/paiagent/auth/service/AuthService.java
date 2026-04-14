package com.paiagent.auth.service;

import com.paiagent.auth.dto.LoginRequest;
import com.paiagent.auth.dto.LoginResponse;
import com.paiagent.auth.dto.RegisterRequest;
import com.paiagent.auth.entity.User;

public interface AuthService {
    LoginResponse register(RegisterRequest req);
    LoginResponse login(LoginRequest req);
    User getUserById(Long userId);
}
