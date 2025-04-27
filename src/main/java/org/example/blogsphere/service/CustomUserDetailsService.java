package org.example.blogsphere.service;

import org.example.blogsphere.entity.User;
import org.example.blogsphere.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new CustomUserDetails(user);
    }

    // Custom UserDetails implementation as inner class or separate class
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {
        private final User user;

        public CustomUserDetails(User user) {
            super(
                    user.getUsername(),
                    user.getPasswordHash(),
                    user.isActive(),
                    true, // account non-expired
                    true, // credentials non-expired
                    true, // account non-locked
                    Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()))
            );
            this.user = user;
        }

        // Custom method to get User ID - not an override
        public Long getId() {
            return user.getId();
        }

        // Get the entire user entity if needed
        public User getUser() {
            return user;
        }
    }
}
