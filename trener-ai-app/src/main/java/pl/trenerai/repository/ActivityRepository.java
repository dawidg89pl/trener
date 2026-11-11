package pl.trenerai.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.trenerai.model.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByUserIdOrderByActivityDateDesc(Long userId);
    Page<Activity> findByUserIdOrderByActivityDateDesc(Long userId, Pageable pageable);
    List<Activity> findByUserIdAndActivityDateBetween(Long userId, LocalDate from, LocalDate to);
    List<Activity> findByIdInAndUserId(List<Long> ids, Long userId);
    Optional<Activity> findByUserIdAndStravaActivityId(Long userId, Long stravaActivityId);
}
