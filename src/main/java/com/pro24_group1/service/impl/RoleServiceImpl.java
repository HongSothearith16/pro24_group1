package com.pro24_group1.service.impl;

import com.pro24_group1.dto.RoleRequest;
import com.pro24_group1.dto.RoleResponse;
import com.pro24_group1.entity.Permission;
import com.pro24_group1.entity.Role;
import com.pro24_group1.entity.RolePermission;
import com.pro24_group1.exception.BadRequestException;
import com.pro24_group1.exception.DuplicateResourceException;
import com.pro24_group1.exception.ResourceNotFoundException;
import com.pro24_group1.mapper.RoleMapper;
import com.pro24_group1.repository.PermissionRepository;
import com.pro24_group1.repository.RolePermissionRepository;
import com.pro24_group1.repository.RoleRepository;
import com.pro24_group1.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAllByDeletedAtIsNull().stream()
                .map(this::enrichRoleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return enrichRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (request.getName() == null || !request.getName().startsWith("ROLE_")) {
            throw new BadRequestException("Role name must start with 'ROLE_'");
        }

        if (roleRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new DuplicateResourceException("Role already exists with name: " + request.getName());
        }

        Role role = roleMapper.toEntity(request);
        Role saved = roleRepository.save(role);
        return enrichRoleResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (request.getName() == null || !request.getName().startsWith("ROLE_")) {
            throw new BadRequestException("Role name must start with 'ROLE_'");
        }

        if (roleRepository.existsByNameAndIdNotAndDeletedAtIsNull(request.getName(), id)) {
            throw new DuplicateResourceException("Role already exists with name: " + request.getName());
        }

        roleMapper.updateEntityFromRequest(request, role);
        Role updated = roleRepository.save(role);
        return enrichRoleResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void grantPermission(Long roleId, Long permId) {
        roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        permissionRepository.findByIdAndDeletedAtIsNull(permId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permId));

        if (rolePermissionRepository.existsByRoleIdAndPermissionIdAndDeletedAtIsNull(roleId, permId)) {
            throw new DuplicateResourceException("Permission is already assigned to this role");
        }

        RolePermission rolePermission = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permId)
                .orElse(null);

        if (rolePermission != null) {
            rolePermission.setDeletedAt(null);
        } else {
            rolePermission = RolePermission.builder()
                    .roleId(roleId)
                    .permissionId(permId)
                    .build();
        }

        rolePermissionRepository.save(rolePermission);
    }

    @Override
    @Transactional
    public void removePermission(Long roleId, Long permId) {
        roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        permissionRepository.findByIdAndDeletedAtIsNull(permId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permId));

        RolePermission rolePermission = rolePermissionRepository.findByRoleIdAndPermissionIdAndDeletedAtIsNull(roleId, permId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission with id " + permId + " is not assigned to role " + roleId));

        rolePermission.setDeletedAt(LocalDateTime.now());
        rolePermissionRepository.save(rolePermission);
    }

    private RoleResponse enrichRoleResponse(Role role) {
        RoleResponse response = roleMapper.toResponse(role);
        List<Long> permIds = rolePermissionRepository.findByRoleIdAndDeletedAtIsNull(role.getId()).stream()
                .map(RolePermission::getPermissionId)
                .toList();

        List<String> permissionNames = permissionRepository.findAllById(permIds).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(Permission::getName)
                .toList();

        response.setPermissions(permissionNames);
        return response;
    }
}
