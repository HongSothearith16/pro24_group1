package com.pro24_group1.service;

import com.pro24_group1.dto.RoleRequest;
import com.pro24_group1.dto.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> getAll();

    RoleResponse getById(Long id);

    RoleResponse create(RoleRequest request);

    RoleResponse update(Long id, RoleRequest request);

    void delete(Long id);

    void grantPermission(Long roleId, Long permId);

    void removePermission(Long roleId, Long permId);
}
