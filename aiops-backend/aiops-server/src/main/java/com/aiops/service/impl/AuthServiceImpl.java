package com.aiops.service.impl;

import com.aiops.dto.LoginDTO;
import com.aiops.dto.RegisterDTO;
import com.aiops.context.BaseContext;
import com.aiops.entity.SysUser;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.SysUserMapper;
import com.aiops.properties.JwtProperties;
import com.aiops.service.AuthService;
import com.aiops.utils.JwtUtil;
import com.aiops.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        validateLogin(loginDTO);
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String token = JwtUtil.createToken(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        ), jwtProperties.getSecret(), jwtProperties.getExpireSeconds());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        validateRegister(registerDTO);
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, registerDTO.getUsername().trim()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername().trim());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setEmail(registerDTO.getEmail());
        user.setRole("merchant");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
    }

    @Override
    public LoginVO profile() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return new LoginVO(null, user.getId(), user.getUsername(), user.getRole());
    }

    private void validateLogin(LoginDTO loginDTO) {
        if (loginDTO == null) {
            throw new BusinessException(400, "登录参数不能为空");
        }
        if (!hasText(loginDTO.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!hasText(loginDTO.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
    }

    private void validateRegister(RegisterDTO registerDTO) {
        if (registerDTO == null) {
            throw new BusinessException(400, "注册参数不能为空");
        }
        if (!hasText(registerDTO.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!hasText(registerDTO.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
