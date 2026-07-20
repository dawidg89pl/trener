package pl.trenerai.repository;
import java.util.List; import org.springframework.data.jpa.repository.JpaRepository; import pl.trenerai.model.GeneratedWorkout;
public interface GeneratedWorkoutRepository extends JpaRepository<GeneratedWorkout,Long>{
    List<GeneratedWorkout> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndDeletedFalse(Long userId);
}
