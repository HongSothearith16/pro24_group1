package com.pro24_group1.service;

import com.pro24_group1.dto.PermissionRequest;
import com.pro24_group1.dto.PermissionResponse;

import java.util.List;

public interface PermissionService {

    List<PermissionResponse> getAll();

    PermissionResponse getById(Long id);

    PermissionResponse create(PermissionRequest request);

    PermissionResponse update(Long id, PermissionRequest request);

    void delete(Long id);
}
