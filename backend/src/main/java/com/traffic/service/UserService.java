package com.traffic.service;

import com.traffic.entity.User;
import com.traffic.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final String TOKEN_PREFIX = "traffic:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    /**
     * 注册新用户
     */
    @Transactional
    public String register(String username, String password, String nickname) {
        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (username == null || username.trim().length() < 3 || username.trim().length() > 20) {
            throw new IllegalArgumentException("用户名长度需在3-20个字符之间");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度至少6位");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .username(username.trim())
                .password(encodedPassword)
                .nickname(nickname != null && !nickname.trim().isEmpty() ? nickname.trim() : username.trim())
                .createTime(LocalDateTime.now())
                .build();

        userMapper.insert(user);
        log.info("用户注册成功: {}", username);

        return generateToken(user);
    }

    /**
     * 登录
     */
    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: {}", username);

        return generateToken(user);
    }

    /**
     * 根据token获取用户信息（Redis不可用时降级，首次返回null让用户重新登录）
     */
    public User getUserByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String key = TOKEN_PREFIX + token;
            String username = redisTemplate.opsForValue().get(key);
            if (username == null) {
                return null;
            }
            redisTemplate.expire(key, TOKEN_TTL);
            return userMapper.selectByUsername(username);
        } catch (Exception e) {
            log.warn("Redis读取token失败（可能未启动），降级处理: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 登出
     */
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            try {
                redisTemplate.delete(TOKEN_PREFIX + token);
            } catch (Exception e) {
                log.warn("Redis删除token失败: {}", e.getMessage());
            }
        }
    }

    private String generateToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(
                    TOKEN_PREFIX + token,
                    user.getUsername(),
                    TOKEN_TTL
            );
        } catch (Exception e) {
            log.warn("Redis存储token失败，用户需每次登录: {}", e.getMessage());
        }
        return token;
    }
}
