package pl.trenerai.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "user_events")
public class UserEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserEvent() {}

    public UserEvent(AppUser user, String type, String description) {
        this.user = user;
        this.type = type;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    @Transient
    public LocalDateTime getCreatedAtLocal() {
        return LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
    }
}
