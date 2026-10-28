package com.pro24_group1.service.impl;

import com.pro24_group1.dto.UserRequest;
import com.pro24_group1.dto.UserResponse;
import com.pro24_group1.dto.UserUpdateRequest;
import com.pro24_group1.entity.Permission;
import com.pro24_group1.entity.Role;
import com.pro24_group1.entity.RolePermission;
import com.pro24_group1.entity.User;
import com.pro24_group1.entity.UserRole;
import com.pro24_group1.exception.DuplicateResourceException;
import com.pro24_group1.exception.ResourceNotFoundException;
import com.pro24_group1.mapper.UserMapper;
import com.pro24_group1.repository.PermissionRepository;
import com.pro24_group1.repository.RolePermissionRepository;
import com.pro24_group1.repository.RoleRepository;
import com.pro24_group1.repository.UserRepository;
import com.pro24_group1.repository.UserRoleRepository;
import com.pro24_group1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllByDeletedAtIsNull().stream()
                .map(this::enrichUserResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return enrichUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                if (roleRepository.existsById(roleId)) {
                    UserRole userRole = UserRole.builder()
                            .userId(savedUser.getId())
                            .roleId(roleId)
                            .build();
                    userRoleRepository.save(userRole);
                }
            }
        }

        return enrichUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (userRepository.existsByUsernameAndIdNotAndDeletedAtIsNull(request.getUsername(), id)) {
                throw new DuplicateResourceException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmailAndIdNotAndDeletedAtIsNull(request.getEmail(), id)) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userMapper.updateEntityFromRequest(request, user);
        User updated = userRepository.save(user);

        if (request.getRoleIds() != null) {
            // Revoke current active roles
            List<UserRole> currentRoles = userRoleRepository.findByUserIdAndDeletedAtIsNull(id);
            for (UserRole ur : currentRoles) {
                ur.setDeletedAt(LocalDateTime.now());
                userRoleRepository.save(ur);
            }
            // Assign new roles
            for (Long roleId : request.getRoleIds()) {
                if (roleRepository.existsById(roleId)) {
                    UserRole ur = UserRole.builder()
                            .userId(id)
                            .roleId(roleId)
                            .build();
                    userRoleRepository.save(ur);
                }
            }
        }

        return enrichUserResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRole(Long userId, Long roleId) {
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)) {
            throw new DuplicateResourceException("Role is already assigned to this user");
        }

        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElse(null);

        if (userRole != null) {
            userRole.setDeletedAt(null);
        } else {
            userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .build();
        }

        userRoleRepository.save(userRole);
    }

    @Override
    @Transactional
    public void revokeRole(Long userId, Long roleId) {
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        UserRole userRole = userRoleRepository.findByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + roleId + " is not assigned to user " + userId));

        userRole.setDeletedAt(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(Long userId) {
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Long> roleIds = userRoleRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(UserRole::getRoleId)
                .toList();

        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> permIds = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> pIds = rolePermissionRepository.findByRoleIdAndDeletedAtIsNull(roleId).stream()
                    .map(RolePermission::getPermissionId)
                    .toList();
            permIds.addAll(pIds);
        }

        if (permIds.isEmpty()) {
            return Collections.emptySet();
        }

        return permissionRepository.findAllById(permIds).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(Permission::getName)
                .collect(Collectors.toSet());
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
