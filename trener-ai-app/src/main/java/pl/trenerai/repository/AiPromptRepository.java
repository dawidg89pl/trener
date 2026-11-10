package pl.trenerai.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.trenerai.model.AiPrompt;

public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {
    List<AiPrompt> findByUserIdOrderByCreatedAtDesc(Long userId);
}
