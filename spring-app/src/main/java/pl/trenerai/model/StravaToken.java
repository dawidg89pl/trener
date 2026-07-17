package pl.trenerai.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "strava_tokens")
public class StravaToken {
    @Id private Long userId;
    @OneToOne @MapsId @JoinColumn(name = "user_id") private AppUser user;
    @Column(nullable = false, columnDefinition = "TEXT") private String accessTokenEncrypted;
    @Column(nullable = false, columnDefinition = "TEXT") private String refreshTokenEncrypted;
    @Column(nullable = false) private Instant expiresAt;
    protected StravaToken() {}
    public StravaToken(AppUser user, String access, String refresh, Instant expiresAt) { this.user=user; accessTokenEncrypted=access; refreshTokenEncrypted=refresh; this.expiresAt=expiresAt; }
    public String getAccessTokenEncrypted(){return accessTokenEncrypted;} public String getRefreshTokenEncrypted(){return refreshTokenEncrypted;} public Instant getExpiresAt(){return expiresAt;}
    public void update(String access,String refresh,Instant expires){accessTokenEncrypted=access;refreshTokenEncrypted=refresh;expiresAt=expires;}
}
