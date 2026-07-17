package pl.trenerai.model;

import jakarta.persistence.*;

@Entity @Table(name = "user_settings")
public class UserSettings {
    @Id private Long userId;
    @OneToOne @MapsId @JoinColumn(name = "user_id") private AppUser user;
    @Column(columnDefinition = "TEXT") private String openAiKeyEncrypted;
    @Column(columnDefinition = "TEXT") private String stravaClientIdEncrypted;
    @Column(columnDefinition = "TEXT") private String stravaClientSecretEncrypted;
    protected UserSettings() {}
    public UserSettings(AppUser user) { this.user = user; }
    public String getOpenAiKeyEncrypted() { return openAiKeyEncrypted; } public void setOpenAiKeyEncrypted(String value) { openAiKeyEncrypted = value; }
    public String getStravaClientIdEncrypted() { return stravaClientIdEncrypted; } public void setStravaClientIdEncrypted(String value) { stravaClientIdEncrypted = value; }
    public String getStravaClientSecretEncrypted() { return stravaClientSecretEncrypted; } public void setStravaClientSecretEncrypted(String value) { stravaClientSecretEncrypted = value; }
}
