package com.aiops.service.impl;

import com.aiops.dto.LoginDTO;
import com.aiops.dto.RegisterDTO;
import com.aiops.entity.SysUser;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.SysUserMapper;
import com.aiops.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(sysUserMapper, passwordEncoder, new JwtProperties());
    }

    @Test
    void loginRejectsMissingPasswordBeforePasswordEncoder() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");

        assertThatThrownBy(() -> authService.login(loginDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void registerRejectsMissingPasswordBeforeInsert() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("new-user");

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
