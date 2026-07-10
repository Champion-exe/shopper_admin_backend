package com.shopper.controller.admin;

import com.shopper.common.Result;
import com.shopper.dto.PageResult;
import com.shopper.entity.User;
import com.shopper.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/list")
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<User> userPage;

        if (keyword != null && !keyword.isEmpty()) {
            userPage = userRepository.findByUsernameContainingOrNicknameContaining(keyword, keyword, pageable);
        } else if (role != null && !role.isEmpty()) {
            userPage = userRepository.findByRole(role, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return Result.success(new PageResult<>(
                userPage.getContent(), userPage.getTotalElements(), page, size));
    }

    @PutMapping("/{id}/status")
    public Result<User> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return Result.error(404, "用户不存在");

        if (body.containsKey("role")) {
            user.setRole(body.get("role"));
        }
        userRepository.save(user);
        return Result.success(user);
    }
}
