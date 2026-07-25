package pl.trenerai.repository;
import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository; import pl.trenerai.model.StravaToken;
public interface StravaTokenRepository extends JpaRepository<StravaToken,Long>{
    Optional<StravaToken> findByStravaAthleteId(Long stravaAthleteId);
}
