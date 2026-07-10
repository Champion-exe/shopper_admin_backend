package com.shopper.controller;

import com.shopper.common.Result;
import com.shopper.dto.LoginRequest;
import com.shopper.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = authService.login(request.getUsername(), request.getPassword());
        if (result == null) {
            return Result.error(401, "用户名或密码错误");
        }
        return Result.success(result);
    }
}
