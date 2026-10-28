package com.pro24_group1.security;

import com.pro24_group1.entity.Permission;
import com.pro24_group1.entity.Role;
import com.pro24_group1.entity.RolePermission;
import com.pro24_group1.entity.User;
import com.pro24_group1.entity.UserRole;
import com.pro24_group1.repository.PermissionRepository;
import com.pro24_group1.repository.RolePermissionRepository;
import com.pro24_group1.repository.RoleRepository;
import com.pro24_group1.repository.UserRepository;
import com.pro24_group1.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();

        List<Long> roleIds = userRoleRepository.findByUserIdAndDeletedAtIsNull(user.getId()).stream()
                .map(UserRole::getRoleId)
                .toList();

        if (!roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findAllById(roleIds).stream()
                    .filter(r -> r.getDeletedAt() == null)
                    .toList();

            for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));

                List<Long> permIds = rolePermissionRepository.findByRoleIdAndDeletedAtIsNull(role.getId()).stream()
                        .map(RolePermission::getPermissionId)
                        .toList();

                if (!permIds.isEmpty()) {
                    List<Permission> permissions = permissionRepository.findAllById(permIds).stream()
                            .filter(p -> p.getDeletedAt() == null)
                            .toList();

                    for (Permission perm : permissions) {
                        authorities.add(new SimpleGrantedAuthority(perm.getName()));
                    }
                }
            }
        }

        return CustomUserDetails.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .enabled(user.isEnabled())
                .authorities(authorities)
                .build();
    }
}
