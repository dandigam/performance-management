package com.rit.performance.security;

import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.User;
import com.rit.performance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String authority = roleAuthority(user.getRole());
        boolean active = "ACTIVE".equalsIgnoreCase(user.getStatus());
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                active,
                java.util.List.of(new SimpleGrantedAuthority(authority)));
    }

    public String roleAuthority(LookupValue role) {
        String roleValue = role == null ? "USER"
                : role.getCode() == null || role.getCode().isBlank()
                        ? role.getName() : role.getCode();
        String normalized = roleValue.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
