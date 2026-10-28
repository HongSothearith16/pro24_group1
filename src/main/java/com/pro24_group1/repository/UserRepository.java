package com.pro24_group1.repository;

import com.pro24_group1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    List<User> findAllByDeletedAtIsNull();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByUsernameAndIdNotAndDeletedAtIsNull(String username, Long id);

    boolean existsByEmailAndIdNotAndDeletedAtIsNull(String email, Long id);
}
