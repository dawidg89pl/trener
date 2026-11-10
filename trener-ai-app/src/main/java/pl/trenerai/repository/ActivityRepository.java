package pl.trenerai.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.trenerai.model.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByUserIdOrderByActivityDateDesc(Long userId);
    List<Activity> findByIdInAndUserId(List<Long> ids, Long userId);
    Optional<Activity> findByUserIdAndStravaActivityId(Long userId, Long stravaActivityId);
}
