package com.aiops.service;

import com.aiops.dto.LoginDTO;
import com.aiops.dto.RegisterDTO;
import com.aiops.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    LoginVO profile();
}

