package pl.trenerai.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.trenerai.model.*;
import pl.trenerai.repository.*;

@Service
public class CurrentUserService {
    private final AppUserRepository users;
    private final UserSettingsRepository settings;

    public CurrentUserService(AppUserRepository users, UserSettingsRepository settings) {
        this.users = users;
        this.settings = settings;
    }

    public AppUser get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    @Transactional
    public UserSettings settings() {
        AppUser user = get();
        return settings.findById(user.getId()).orElseGet(() -> settings.save(new UserSettings(user)));
    }
}
