package com.pro24_group1.service;

import com.pro24_group1.dto.AuthResponse;
import com.pro24_group1.dto.LoginRequest;
import com.pro24_group1.dto.RefreshTokenRequest;
import com.pro24_group1.dto.RegisterRequest;
import com.pro24_group1.dto.UserProfileResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    UserProfileResponse getCurrentUser();
}
