package pl.trenerai.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "strava_tokens")
public class StravaToken {
    @Id private Long userId;
    @OneToOne @MapsId @JoinColumn(name = "user_id") private AppUser user;
    @Column(unique = true) private Long stravaAthleteId;
    @Column(nullable = false, columnDefinition = "TEXT") private String accessTokenEncrypted;
    @Column(nullable = false, columnDefinition = "TEXT") private String refreshTokenEncrypted;
    @Column(nullable = false) private Instant expiresAt;
    protected StravaToken() {}
    public StravaToken(AppUser user, Long athleteId, String access, String refresh, Instant expiresAt) { this.user=user;stravaAthleteId=athleteId;accessTokenEncrypted=access;refreshTokenEncrypted=refresh;this.expiresAt=expiresAt; }
    public Long getUserId(){return userId;} public Long getStravaAthleteId(){return stravaAthleteId;} public String getAccessTokenEncrypted(){return accessTokenEncrypted;} public String getRefreshTokenEncrypted(){return refreshTokenEncrypted;} public Instant getExpiresAt(){return expiresAt;}
    public void update(Long athleteId,String access,String refresh,Instant expires){if(athleteId!=null)stravaAthleteId=athleteId;accessTokenEncrypted=access;refreshTokenEncrypted=refresh;expiresAt=expires;}
}
