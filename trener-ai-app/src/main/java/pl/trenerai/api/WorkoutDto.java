package pl.trenerai.api;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.trenerai.model.GeneratedWorkout;
import pl.trenerai.service.WorkoutSummaryService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Wygenerowany trening z planu treningowego")
public record WorkoutDto(
        @Schema(description = "ID treningu", example = "7")
        Long id,
        @Schema(description = "Nazwa treningu", example = "Sweet Spot Intervals")
        String name,
        @Schema(description = "Planowana data wykonania", example = "2026-11-12")
        LocalDate plannedDate,
        @Schema(description = "Data utworzenia")
        Instant createdAt,
        @Schema(description = "Podsumowanie treningu")
        SummaryDto summary
) {
    public static WorkoutDto from(GeneratedWorkout w, WorkoutSummaryService summaryService) {
        var s = summaryService.summarize(w);
        return new WorkoutDto(w.getId(), w.getName(), w.getPlannedDate(), w.getCreatedAt(),
                new SummaryDto(s.description(), s.duration(),
                        s.zones().stream().map(z -> new ZoneDto(z.number(), z.label(), z.range(),
                                z.duration(), z.percentage())).toList(),
                        s.steps().stream().map(st -> new StepDto(st.label(), st.duration(),
                                st.power(), st.cadence())).toList()));
    }

    @Schema(description = "Podsumowanie treningu")
    public record SummaryDto(
            @Schema(description = "Opis treningu") String description,
            @Schema(description = "Czas trwania", example = "1 h 15 min") String duration,
            @Schema(description = "Rozkład stref mocy") List<ZoneDto> zones,
            @Schema(description = "Kroki treningu") List<StepDto> steps) {}

    @Schema(description = "Strefa mocy")
    public record ZoneDto(
            @Schema(description = "Numer strefy", example = "3") int number,
            @Schema(description = "Etykieta", example = "Z3 Tempo") String label,
            @Schema(description = "Zakres mocy", example = "76–90% FTP") String range,
            @Schema(description = "Czas w strefie", example = "25 min") String duration,
            @Schema(description = "Procent czasu", example = "33.3") double percentage) {}

    @Schema(description = "Krok treningu")
    public record StepDto(
            @Schema(description = "Typ kroku", example = "Rozgrzewka") String label,
            @Schema(description = "Czas trwania", example = "10 min") String duration,
            @Schema(description = "Moc", example = "88% FTP") String power,
            @Schema(description = "Kadencja", example = "90 rpm") String cadence) {}
}
