package pl.trenerai.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.trenerai.model.AppUser;
import pl.trenerai.model.UserSettings;
import pl.trenerai.repository.AppUserRepository;
import pl.trenerai.repository.UserSettingsRepository;

@Service
public class UserBootstrapService {
    private final AppUserRepository users; private final UserSettingsRepository settings; private final PasswordEncoder passwords;
    public UserBootstrapService(AppUserRepository users, UserSettingsRepository settings, PasswordEncoder passwords) { this.users=users; this.settings=settings; this.passwords=passwords; }
    @Transactional public void createIfMissing(String email, String password) {
        if (users.findByEmailIgnoreCase(email).isEmpty()) {
            AppUser user = users.save(new AppUser(email.trim().toLowerCase(), passwords.encode(password)));
            settings.save(new UserSettings(user));
        }
    }
}
