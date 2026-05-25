package com.traffic.controller;

import com.traffic.entity.User;
import com.traffic.service.UserService;
import com.traffic.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");

        try {
            String token = userService.register(username, password, nickname);
            User user = userService.getUserByToken(token);
            if (user == null) {
                return ApiResponse.error(500, "注册成功但获取用户信息失败，请重新登录");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("userId", user.getId());

            return ApiResponse.success(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("注册失败", e);
            return ApiResponse.error(500, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            String token = userService.login(username, password);
            User user = userService.getUserByToken(token);
            if (user == null) {
                return ApiResponse.error(500, "登录成功但获取用户信息失败，请重试");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("userId", user.getId());

            return ApiResponse.success(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("登录失败", e);
            return ApiResponse.error(500, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }

        String token = authHeader.substring(7);
        User user = userService.getUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "登录已过期");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());

        return ApiResponse.success(data);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userService.logout(authHeader.substring(7));
        }
        return ApiResponse.success(null);
    }
}
