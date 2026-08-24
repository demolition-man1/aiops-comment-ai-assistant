package com.aiops.controller;

import com.aiops.dto.LoginDTO;
import com.aiops.dto.RegisterDTO;
import com.aiops.result.Result;
import com.aiops.service.AuthService;
import com.aiops.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "登录、注册和当前用户信息")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT 访问令牌和用户信息")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @PostMapping("/auth/register")
    @Operation(summary = "用户注册", description = "创建新的后台用户账号")
    public Result<Void> register(@RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success();
    }

    @GetMapping("/user/profile")
    @Operation(summary = "当前用户信息", description = "根据 JWT 获取当前登录用户信息")
    public Result<LoginVO> profile() {
        return Result.success(authService.profile());
    }
}
