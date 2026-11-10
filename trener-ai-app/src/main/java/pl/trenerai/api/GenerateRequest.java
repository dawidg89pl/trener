package pl.trenerai.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Żądanie wygenerowania planu treningowego")
public record GenerateRequest(
        @NotEmpty(message = "Wybierz co najmniej jedną aktywność.")
        @Schema(description = "Lista ID aktywności do analizy", example = "[1, 2, 3]")
        List<Long> activityIds,

        @NotNull(message = "Podaj datę rozpoczęcia planu.")
        @Schema(description = "Data rozpoczęcia planu", example = "2026-11-11")
        LocalDate startDate,

        @NotNull(message = "Wybierz cel treningowy.")
        @Schema(description = "Cel treningowy: improve_ftp lub long_distance_endurance", example = "improve_ftp")
        String trainingGoal,

        @NotNull(message = "Podaj tygodniowy czas na trening.")
        @DecimalMin(value = "1", message = "Minimalny czas to 1 godzina.")
        @DecimalMax(value = "40", message = "Maksymalny czas to 40 godzin.")
        @Schema(description = "Dostępny czas na trening tygodniowo (w godzinach)", example = "8.5")
        BigDecimal weeklyTrainingHours
) {}
