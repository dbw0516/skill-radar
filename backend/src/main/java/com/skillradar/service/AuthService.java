package com.skillradar.service;

import com.skillradar.dto.AuthUser;
import com.skillradar.dto.LoginRequest;
import com.skillradar.dto.RegisterRequest;
import com.skillradar.entity.User;
import com.skillradar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthUser register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这个邮箱已经注册过了");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setMajorId(req.getMajorId());
        user.setLocation(req.getLocation());
        user.setTargetLocation(req.getTargetLocation());
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 兜底：两个人几乎同时用同一个邮箱注册，前面 findByEmail 检查有个短暂的竞态窗口
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这个邮箱已经注册过了");
        }
        return toAuthUser(user);
    }

    public AuthUser login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码不对"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码不对");
        }
        return toAuthUser(user);
    }

    public AuthUser toAuthUser(User u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getNickname(), u.getMajorId(), u.getTargetCategoryId(),
                u.getLocation(), u.getTargetLocation());
    }
}
