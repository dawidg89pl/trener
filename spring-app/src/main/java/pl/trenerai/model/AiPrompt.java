package pl.trenerai.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_prompts")
public class AiPrompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AiPrompt() {}

    public AiPrompt(AppUser user, String prompt) {
        this.user = user;
        this.prompt = prompt;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getPrompt() { return prompt; }
    public Instant getCreatedAt() { return createdAt; }
}
