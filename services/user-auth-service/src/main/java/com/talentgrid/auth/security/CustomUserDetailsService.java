package com.talentgrid.auth.security;

import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load user details from the database during username/password authentication</li>
 *   <li>Convert entity roles and scopes into Spring Security authorities</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Locates the user based on the email address.
     *
     * @param email the email address identifying the user
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        // Add Roles
        user.getRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()))
        );

        // Add Scopes
        user.getRoles().forEach(role ->
                role.getScopes().forEach(scope ->
                        authorities.add(new SimpleGrantedAuthority(scope.getName()))
                )
        );

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                // Spring Security built-in checks
                .disabled(!user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .build();
    }
}