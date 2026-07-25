package pl.trenerai.model;

import jakarta.persistence.*;

@Entity @Table(name = "user_settings")
public class UserSettings {
    @Id private Long userId;
    @OneToOne @MapsId @JoinColumn(name = "user_id") private AppUser user;
    @Column(columnDefinition = "TEXT") private String openAiKeyEncrypted;
    @Column private String aiProvider = "openai";
    @Column private String openAiModel = "gpt-4o";
    @Column private Integer stravaFtp;
    protected UserSettings() {}
    public UserSettings(AppUser user) { this.user = user; }
    public String getOpenAiKeyEncrypted() { return openAiKeyEncrypted; } public void setOpenAiKeyEncrypted(String value) { openAiKeyEncrypted = value; }
    public String getAiProvider() { return aiProvider == null || aiProvider.isBlank() ? "openai" : aiProvider; }
    public void setAiProvider(String value) { aiProvider = value; }
    public String getOpenAiModel() { return openAiModel == null || openAiModel.isBlank() ? "gpt-4o" : openAiModel; }
    public void setOpenAiModel(String value) { openAiModel = value; }
    public Integer getStravaFtp() { return stravaFtp; } public void setStravaFtp(Integer value) { stravaFtp = value; }
}
