package pl.trenerai.api;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.trenerai.model.Activity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Aktywność sportowa zsynchronizowana ze Stravy")
public record ActivityDto(
        @Schema(description = "ID aktywności", example = "42")
        Long id,
        @Schema(description = "ID aktywności w Strava", example = "12345678900")
        Long stravaActivityId,
        @Schema(description = "Nazwa aktywności", example = "Poranny trening")
        String name,
        @Schema(description = "Typ aktywności", example = "Ride")
        String activityType,
        @Schema(description = "Typ sportu", example = "MountainBikeRide")
        String sportType,
        @Schema(description = "Data aktywności", example = "2026-11-08")
        LocalDate activityDate,
        @Schema(description = "Czas rozpoczęcia (czas lokalny)")
        LocalDateTime startDateLocal,
        @Schema(description = "Dystans w km", example = "45.2")
        double distanceKm,
        @Schema(description = "Czas ruchu w minutach", example = "92")
        int movingTimeMin,
        @Schema(description = "Czas całkowity w minutach", example = "105")
        int elapsedTimeMin,
        @Schema(description = "Przewyższenie w metrach", example = "620.0")
        double elevationGainM,
        @Schema(description = "Średnia prędkość w km/h", example = "29.5")
        Double averageSpeedKph,
        @Schema(description = "Średnia moc w watach", example = "195.0")
        Double averageWatts,
        @Schema(description = "Ważona średnia moc w watach", example = "210.0")
        Double weightedAverageWatts,
        @Schema(description = "Maksymalna moc w watach", example = "650")
        Integer maxWatts,
        @Schema(description = "Średnia kadencja", example = "88.0")
        Double averageCadence,
        @Schema(description = "Średnie tętno", example = "142.0")
        Double averageHeartRate,
        @Schema(description = "Maksymalne tętno", example = "178.0")
        Double maxHeartRate,
        @Schema(description = "Kalorie", example = "820.0")
        Double calories,
        @Schema(description = "Czy aktywność była już analizowana przez AI")
        boolean analyzed
) {
    public static ActivityDto from(Activity a) {
        return new ActivityDto(
                a.getId(), a.getStravaActivityId(), a.getName(), a.getActivityType(),
                a.getSportType(), a.getActivityDate(), a.getStartDateLocal(),
                a.getDistanceKm(), a.getMovingTimeMin(), a.getElapsedTimeMin(),
                a.getElevationGainM(), a.getAverageSpeedKph(), a.getAverageWatts(),
                a.getWeightedAverageWatts(), a.getMaxWatts(), a.getAverageCadence(),
                a.getAverageHeartRate(), a.getMaxHeartRate(), a.getCalories(),
                a.isAnalyzed());
    }
}
