package com.hoang.ddd.infrastructure.security;

import com.hoang.ddd.domain.model.entity.Permission;
import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ADAPTER PATTERN: Chuyển đổi Domain Entity (User) thành Spring Security
 * contract (UserDetails)
 */
public class UserDetailsCustom implements UserDetails {

    private final User user;
    private final Set<GrantedAuthority> authorities;

    public UserDetailsCustom(User user) {
        this.user = user;
        this.authorities = mapRolesAndPermissionsToAuthorities(user.getRoles());
    }

    private Set<GrantedAuthority> mapRolesAndPermissionsToAuthorities(Set<Role> roles) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        if (roles == null) {
            return grantedAuthorities;
        }

        for (Role role : roles) {
            // 1. Thêm Role (ví dụ: ROLE_USER)
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));

            // 2. Thêm các Permissions tương ứng của Role đó (ví dụ: ticket:book)
            if (role.getPermissions() != null) {
                for (Permission permission : role.getPermissions()) {
                    grantedAuthorities.add(new SimpleGrantedAuthority(permission.getName()));
                }
            }
        }
        return grantedAuthorities;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Tài khoản không bị khóa nếu status khác LOCKED
        return user.getStatus() != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Tài khoản hoạt động khi status là ACTIVE
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
