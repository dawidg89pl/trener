package pl.trenerai.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pl.trenerai.model.StravaToken;
public interface StravaTokenRepository extends JpaRepository<StravaToken,Long>{}
