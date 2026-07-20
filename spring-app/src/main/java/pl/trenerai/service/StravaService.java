package pl.trenerai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.trenerai.model.Activity;
import pl.trenerai.model.AppUser;
import pl.trenerai.model.StravaToken;
import pl.trenerai.model.UserSettings;
import pl.trenerai.repository.ActivityRepository;
import pl.trenerai.repository.StravaTokenRepository;
import pl.trenerai.repository.UserSettingsRepository;

@Service
public class StravaService {
    public record SyncResult(int activities, int detailed, int withPower, int withHeartRate, int withCadence) {}

    private static final Set<String> CYCLING_ACTIVITY_TYPES = Set.of(
            "Ride",
            "MountainBikeRide",
            "GravelRide",
            "VirtualRide",
            "EBikeRide",
            "EMountainBikeRide",
            "Velomobile",
            "Handcycle");

    private final CurrentUserService current;
    private final UserSettingsRepository settings;
    private final StravaTokenRepository tokens;
    private final ActivityRepository activities;
    private final SecretCrypto crypto;
    private final UserEventService eventLog;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String publicUrl;

    public StravaService(CurrentUserService c, UserSettingsRepository s, StravaTokenRepository t,
            ActivityRepository a, SecretCrypto crypto, UserEventService eventLog,
            @Value("${app.public-url}") String url) {
        current = c;
        settings = s;
        tokens = t;
        activities = a;
        this.crypto = crypto;
        this.eventLog = eventLog;
        publicUrl = url.replaceAll("/$", "");
    }

    public String authorizationUrl() {
        var set = current.settings();
        String client = crypto.decrypt(set.getStravaClientIdEncrypted());
        if (client.isBlank()) {
            throw new IllegalStateException("Najpierw zapisz Client ID oraz Client Secret Stravy w ustawieniach.");
        }
        return "https://www.strava.com/oauth/authorize?" + form(Map.of(
                "client_id", client,
                "redirect_uri", publicUrl + "/strava/callback",
                "response_type", "code",
                "scope", "profile:read_all,activity:read_all"));
    }

    @Transactional
    public void callback(String code) throws Exception {
        AppUser user = current.get();
        var set = current.settings();
        JsonNode data = post("https://www.strava.com/oauth/token", Map.of(
                "client_id", crypto.decrypt(set.getStravaClientIdEncrypted()),
                "client_secret", crypto.decrypt(set.getStravaClientSecretEncrypted()),
                "code", code,
                "grant_type", "authorization_code"));
        saveToken(user, data);
        updateAthleteFtp(current.settings(), data.path("access_token").asText());
        eventLog.record(user, "STRAVA_CONNECTED", "Połączono konto ze Stravą");
    }

    @Transactional
    public SyncResult syncActivities() throws Exception {
        AppUser user = current.get();
        StravaToken token = tokens.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Najpierw połącz konto Strava."));
        if (token.getExpiresAt().isBefore(Instant.now().plusSeconds(60))) {
            tokenRefresh(user, current.settings(), token);
        }

        String accessToken = crypto.decrypt(token.getAccessTokenEncrypted());
        updateAthleteFtp(current.settings(), accessToken);
        JsonNode summaries = get("https://www.strava.com/api/v3/athlete/activities?per_page=30", accessToken);
        if (!summaries.isArray()) {
            throw new IllegalStateException("Strava nie zwróciła listy aktywności.");
        }

        int detailed = 0;
        int withPower = 0;
        int withHeartRate = 0;
        int withCadence = 0;
        int cyclingActivities = 0;
        for (JsonNode summary : summaries) {
            if (!isCyclingActivity(summary)) {
                continue;
            }
            cyclingActivities++;
            long remoteId = summary.path("id").asLong();
            JsonNode source = summary;
            try {
                source = get("https://www.strava.com/api/v3/activities/" + remoteId
                        + "?include_all_efforts=false", accessToken);
                detailed++;
            } catch (IllegalStateException ignored) {
                // Dane podsumowania nadal pozwalają zapisać większość metryk treningowych.
            }

            Activity activity = activities.findByUserIdAndStravaActivityId(user.getId(), remoteId)
                    .orElseGet(() -> new Activity(user, remoteId));
            updateActivity(activity, source);
            activities.save(activity);
            if (activity.getAverageWatts() != null || activity.getWeightedAverageWatts() != null) withPower++;
            if (activity.getAverageHeartRate() != null || activity.getMaxHeartRate() != null) withHeartRate++;
            if (activity.getAverageCadence() != null) withCadence++;
        }
        eventLog.record(user, "STRAVA_SYNC", "Zsynchronizowano ze Stravą "
                + cyclingActivities + " aktywności rowerowych");
        return new SyncResult(cyclingActivities, detailed, withPower, withHeartRate, withCadence);
    }

    private boolean isCyclingActivity(JsonNode activity) {
        return CYCLING_ACTIVITY_TYPES.contains(activity.path("sport_type").asText())
                || CYCLING_ACTIVITY_TYPES.contains(activity.path("type").asText());
    }

    private void updateActivity(Activity activity, JsonNode row) {
        LocalDateTime start = parseLocalDateTime(row.path("start_date_local").asText(
                row.path("start_date").asText()));
        activity.update(
                row.path("name").asText("Bez nazwy"),
                text(row, "description"),
                row.path("type").asText("Ride"),
                row.path("sport_type").asText(row.path("type").asText("Ride")),
                start.toLocalDate(),
                start,
                row.path("distance").asDouble() / 1000.0,
                secondsToMinutes(row.path("moving_time").asInt()),
                secondsToMinutes(row.path("elapsed_time").asInt()),
                row.path("total_elevation_gain").asDouble(),
                number(row, "elev_high"),
                number(row, "elev_low"),
                metersPerSecondToKph(number(row, "average_speed")),
                metersPerSecondToKph(number(row, "max_speed")),
                number(row, "average_cadence"),
                number(row, "average_watts"),
                number(row, "weighted_average_watts"),
                integer(row, "max_watts"),
                number(row, "kilojoules"),
                number(row, "average_heartrate"),
                number(row, "max_heartrate"),
                number(row, "calories"),
                number(row, "average_temp"),
                integer(row, "perceived_exertion"),
                integer(row, "suffer_score"),
                integer(row, "workout_type"),
                text(row, "device_name"),
                bool(row, "device_watts"),
                row.path("trainer").asBoolean(false),
                row.path("commute").asBoolean(false),
                row.path("manual").asBoolean(false));
    }

    private void updateAthleteFtp(UserSettings userSettings, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return;
        try {
            JsonNode athlete = get("https://www.strava.com/api/v3/athlete", accessToken);
            JsonNode ftp = athlete.get("ftp");
            if (ftp != null) {
                userSettings.setStravaFtp(ftp.isNumber() && ftp.asInt() > 0 ? ftp.asInt() : null);
                settings.save(userSettings);
            }
        } catch (Exception ignored) {
            // Brak dostępu do profilu nie powinien blokować synchronizacji aktywności.
        }
    }

    private JsonNode get(String url, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Strava odrzuciła pobieranie danych (HTTP "
                    + response.statusCode() + ").");
        }
        return json.readTree(response.body());
    }

    private void tokenRefresh(AppUser user, UserSettings set, StravaToken token) throws Exception {
        JsonNode data = post("https://www.strava.com/oauth/token", Map.of(
                "client_id", crypto.decrypt(set.getStravaClientIdEncrypted()),
                "client_secret", crypto.decrypt(set.getStravaClientSecretEncrypted()),
                "refresh_token", crypto.decrypt(token.getRefreshTokenEncrypted()),
                "grant_type", "refresh_token"));
        saveToken(user, data);
    }

    private void saveToken(AppUser user, JsonNode data) {
        String access = crypto.encrypt(data.path("access_token").asText());
        String refresh = crypto.encrypt(data.path("refresh_token").asText());
        Instant expires = Instant.ofEpochSecond(data.path("expires_at").asLong());
        tokens.findById(user.getId()).ifPresentOrElse(existing -> {
            existing.update(access, refresh, expires);
            tokens.save(existing);
        }, () -> tokens.save(new StravaToken(user, access, refresh, expires)));
    }

    private JsonNode post(String url, Map<String, String> values) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(values))).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            JsonNode error = json.readTree(response.body());
            String detail = error.path("message").asText(error.path("errors").toString());
            throw new IllegalStateException("Strava odrzuciła połączenie (HTTP "
                    + response.statusCode() + "): " + detail);
        }
        return json.readTree(response.body());
    }

    private String form(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "&" + right).orElse("");
    }

    private LocalDateTime parseLocalDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return LocalDateTime.parse(value);
        }
    }

    private int secondsToMinutes(int seconds) { return Math.max(0, (int) Math.round(seconds / 60.0)); }
    private Double metersPerSecondToKph(Double value) { return value == null ? null : value * 3.6; }
    private Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asDouble();
    }
    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asInt();
    }
    private Boolean bool(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isBoolean() ? null : value.asBoolean();
    }
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()
                ? null : value.asText();
    }
}
