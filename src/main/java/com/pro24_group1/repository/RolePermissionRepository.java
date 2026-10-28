package com.pro24_group1.repository;

import com.pro24_group1.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    List<RolePermission> findByRoleIdAndDeletedAtIsNull(Long roleId);

    List<RolePermission> findByPermissionIdAndDeletedAtIsNull(Long permissionId);

    Optional<RolePermission> findByRoleIdAndPermissionIdAndDeletedAtIsNull(Long roleId, Long permissionId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    boolean existsByRoleIdAndPermissionIdAndDeletedAtIsNull(Long roleId, Long permissionId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
