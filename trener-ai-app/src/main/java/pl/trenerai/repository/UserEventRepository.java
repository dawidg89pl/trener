package pl.trenerai.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.trenerai.model.UserEvent;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {
    List<UserEvent> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<UserEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
