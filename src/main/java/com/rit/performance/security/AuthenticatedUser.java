package com.rit.performance.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/** Security principal containing the database user ID needed by JPA auditing. */
public record AuthenticatedUser(
        Long id,
        String username,
        String password,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
