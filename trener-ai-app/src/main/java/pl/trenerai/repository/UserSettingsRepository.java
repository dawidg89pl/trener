package pl.trenerai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.trenerai.model.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
