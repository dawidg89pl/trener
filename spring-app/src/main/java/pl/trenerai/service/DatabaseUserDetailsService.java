package pl.trenerai.service;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import pl.trenerai.repository.AppUserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    public UserDetails loadUserByUsername(String email) {
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Nie ma takiego konta."));
        return User.withUsername(user.getEmail()).password(user.getPasswordHash()).roles("USER").build();
    }
}
