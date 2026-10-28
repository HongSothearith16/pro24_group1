package com.pro24_group1.repository;

import com.pro24_group1.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByUserIdAndDeletedAtIsNull(Long userId);

    List<UserRole> findByRoleIdAndDeletedAtIsNull(Long roleId);

    Optional<UserRole> findByUserIdAndRoleIdAndDeletedAtIsNull(Long userId, Long roleId);

    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByUserIdAndRoleIdAndDeletedAtIsNull(Long userId, Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
