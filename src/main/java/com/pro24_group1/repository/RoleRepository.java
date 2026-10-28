package com.pro24_group1.repository;

import com.pro24_group1.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> findByNameAndDeletedAtIsNull(String name);

    Optional<Role> findByIdAndDeletedAtIsNull(Long id);

    List<Role> findAllByDeletedAtIsNull();

    boolean existsByName(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, Long id);
}
