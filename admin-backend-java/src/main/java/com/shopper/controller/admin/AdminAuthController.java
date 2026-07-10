package com.shopper.controller.admin;

import com.shopper.common.Result;
import com.shopper.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        Map<String, Object> result = authService.login(username, password);
        if (result == null) {
            return Result.error(401, "用户名或密码错误");
        }

        // 管理员后台只允许 admin/seller 角色
        Object userObj = result.get("user");
        String role = "";
        if (userObj instanceof com.shopper.entity.User) {
            role = ((com.shopper.entity.User) userObj).getRole();
        } else if (userObj instanceof Map) {
            role = (String) ((Map<?, ?>) userObj).get("role");
        }
        if (!"admin".equals(role) && !"seller".equals(role)) {
            return Result.error(403, "无权限访问管理后台");
        }

        return Result.success(result);
    }
}
