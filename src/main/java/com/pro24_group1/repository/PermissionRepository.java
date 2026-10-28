package com.pro24_group1.repository;

import com.pro24_group1.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    Optional<Permission> findByNameAndDeletedAtIsNull(String name);

    Optional<Permission> findByIdAndDeletedAtIsNull(Long id);

    List<Permission> findAllByDeletedAtIsNull();

    boolean existsByName(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, Long id);
}
