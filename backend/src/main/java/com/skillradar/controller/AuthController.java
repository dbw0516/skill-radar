package com.skillradar.controller;

import com.skillradar.dto.AuthUser;
import com.skillradar.dto.LoginRequest;
import com.skillradar.dto.ProfileUpdateRequest;
import com.skillradar.dto.RegisterRequest;
import com.skillradar.entity.User;
import com.skillradar.repository.UserRepository;
import com.skillradar.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public AuthUser register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthUser login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 前端"选择目标岗位"时调用，把选择持久化到这个用户身上，而不只是留在浏览器内存里。 */
    @PutMapping("/users/{id}/target-category")
    public AuthUser setTargetCategory(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        User user = findUser(id);
        user.setTargetCategoryId(body.get("categoryId"));
        return authService.toAuthUser(userRepository.save(user));
    }

    /** "个人中心"页保存资料：昵称、专业、所在地区、意向就业地区。 */
    @PutMapping("/users/{id}/profile")
    public AuthUser updateProfile(@PathVariable Long id, @RequestBody ProfileUpdateRequest req) {
        User user = findUser(id);
        user.setNickname(req.getNickname());
        user.setMajorId(req.getMajorId());
        user.setLocation(req.getLocation());
        user.setTargetLocation(req.getTargetLocation());
        return authService.toAuthUser(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
}
