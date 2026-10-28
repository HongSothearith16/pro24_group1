package com.pro24_group1.service.impl;

import com.pro24_group1.dto.AuthResponse;
import com.pro24_group1.dto.LoginRequest;
import com.pro24_group1.dto.RefreshTokenRequest;
import com.pro24_group1.dto.RegisterRequest;
import com.pro24_group1.dto.UserProfileResponse;
import com.pro24_group1.dto.UserResponse;
import com.pro24_group1.entity.Role;
import com.pro24_group1.entity.User;
import com.pro24_group1.entity.UserRole;
import com.pro24_group1.exception.BadRequestException;
import com.pro24_group1.exception.DuplicateResourceException;
import com.pro24_group1.exception.ResourceNotFoundException;
import com.pro24_group1.mapper.UserMapper;
import com.pro24_group1.repository.RoleRepository;
import com.pro24_group1.repository.UserRepository;
import com.pro24_group1.repository.UserRoleRepository;
import com.pro24_group1.security.CustomUserDetails;
import com.pro24_group1.security.CustomUserDetailsService;
import com.pro24_group1.security.JwtService;
import com.pro24_group1.service.AuthService;
import com.pro24_group1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Assign default role ROLE_USER if exists
        roleRepository.findByNameAndDeletedAtIsNull("ROLE_USER").ifPresent(role -> {
            UserRole userRole = UserRole.builder()
                    .userId(savedUser.getId())
                    .roleId(role.getId())
                    .build();
            userRoleRepository.save(userRole);
        });

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(savedUser.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails, savedUser.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails, savedUser.getId());

        UserResponse userResponse = enrichUserResponse(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findByIdAndDeletedAtIsNull(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = jwtService.generateAccessToken(userDetails, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());

        UserResponse userResponse = enrichUserResponse(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtService.validateToken(request.getRefreshToken())) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(request.getRefreshToken());
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

        User user = userRepository.findByIdAndDeletedAtIsNull(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getId());

        UserResponse userResponse = enrichUserResponse(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("Current user not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        List<Long> roleIds = userRoleRepository.findByUserIdAndDeletedAtIsNull(user.getId()).stream()
                .map(UserRole::getRoleId)
                .toList();

        List<String> roles = roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(Role::getName)
                .toList();

        Set<String> permissions = userService.getEffectivePermissions(user.getId());

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .enabled(user.isEnabled())
                .roles(roles)
                .permissions(permissions)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserResponse enrichUserResponse(User user) {
        UserResponse response = userMapper.toResponse(user);
        List<Long> roleIds = userRoleRepository.findByUserIdAndDeletedAtIsNull(user.getId()).stream()
                .map(UserRole::getRoleId)
                .toList();

        List<String> roleNames = roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(Role::getName)
                .toList();

        response.setRoles(roleNames);
        return response;
    }
}
