package com.thoonidev.InventoryMgtSystem.security;

import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("User Email Not Found"));
        if (user.getOrganization() != null && Boolean.FALSE.equals(user.getOrganization().getActive())) {
            throw new UsernameNotFoundException("Organization inactive");
        }

        return AuthUser.builder()
                .user(user)
                .build();
    }
}
