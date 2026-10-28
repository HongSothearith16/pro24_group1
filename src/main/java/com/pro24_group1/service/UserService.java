package com.pro24_group1.service;

import com.pro24_group1.dto.UserRequest;
import com.pro24_group1.dto.UserResponse;
import com.pro24_group1.dto.UserUpdateRequest;

import java.util.List;
import java.util.Set;

public interface UserService {

    List<UserResponse> getAll();

    UserResponse getById(Long id);

    UserResponse create(UserRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    void delete(Long id);

    void assignRole(Long userId, Long roleId);

    void revokeRole(Long userId, Long roleId);

    Set<String> getEffectivePermissions(Long userId);
}
