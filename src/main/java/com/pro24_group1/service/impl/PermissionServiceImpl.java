package com.pro24_group1.service.impl;

import com.pro24_group1.dto.PermissionRequest;
import com.pro24_group1.dto.PermissionResponse;
import com.pro24_group1.entity.Permission;
import com.pro24_group1.exception.DuplicateResourceException;
import com.pro24_group1.exception.ResourceNotFoundException;
import com.pro24_group1.mapper.PermissionMapper;
import com.pro24_group1.repository.PermissionRepository;
import com.pro24_group1.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAllByDeletedAtIsNull().stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getById(Long id) {
        Permission permission = permissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));
        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new DuplicateResourceException("Permission already exists with name: " + request.getName());
        }

        Permission permission = permissionMapper.toEntity(request);
        Permission saved = permissionRepository.save(permission);
        return permissionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request) {
        Permission permission = permissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        if (permissionRepository.existsByNameAndIdNotAndDeletedAtIsNull(request.getName(), id)) {
            throw new DuplicateResourceException("Permission already exists with name: " + request.getName());
        }

        permissionMapper.updateEntityFromRequest(request, permission);
        Permission updated = permissionRepository.save(permission);
        return permissionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        permission.setDeletedAt(LocalDateTime.now());
        permissionRepository.save(permission);
    }
}
