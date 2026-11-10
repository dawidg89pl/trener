package pl.trenerai.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.trenerai.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
}
