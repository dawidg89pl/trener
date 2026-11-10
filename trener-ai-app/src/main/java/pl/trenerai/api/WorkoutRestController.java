package pl.trenerai.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.service.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/workouts")
@Tag(name = "Treningi", description = "Generowanie i zarządzanie planami treningowymi")
public class WorkoutRestController {

    private final CurrentUserService current;
    private final GeneratedWorkoutRepository workouts;
    private final WorkoutGenerationService generator;
    private final WorkoutSummaryService summaryService;
    private final FitWorkoutService fitService;

    public WorkoutRestController(CurrentUserService current, GeneratedWorkoutRepository workouts,
            WorkoutGenerationService generator, WorkoutSummaryService summaryService,
            FitWorkoutService fitService) {
        this.current = current;
        this.workouts = workouts;
        this.generator = generator;
        this.summaryService = summaryService;
        this.fitService = fitService;
    }

    @GetMapping
    @Operation(summary = "Aktywny plan treningowy", description = "Zwraca listę treningów z aktywnego planu")
    public List<WorkoutDto> list() {
        return workouts.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(current.get().getId())
                .stream().map(w -> WorkoutDto.from(w, summaryService)).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły treningu", description = "Zwraca szczegóły pojedynczego treningu z podsumowaniem")
    public WorkoutDto get(@PathVariable Long id) {
        return workouts.findById(id)
                .filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .map(w -> WorkoutDto.from(w, summaryService))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trening nie został znaleziony."));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generuj plan treningowy",
            description = "Generuje 7-dniowy plan treningowy na podstawie wybranych aktywności i parametrów. "
                    + "Wymaga połączonego konta Strava, skonfigurowanego klucza API dostawcy AI oraz braku aktywnego planu.")
    public ApiResponse generate(@Valid @RequestBody GenerateRequest request) {
        try {
            int count = generator.generate(
                    request.activityIds(), request.startDate(),
                    WorkoutGenerationService.TrainingGoal.fromId(request.trainingGoal()),
                    request.weeklyTrainingHours());
            return new ApiResponse("Wygenerowano plan na 7 dni: " + count + " treningów.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Nie udało się wygenerować planu treningowego.");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usuń trening", description = "Oznacza trening jako usunięty (soft delete)")
    public ApiResponse delete(@PathVariable Long id) {
        var workout = workouts.findById(id)
                .filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trening nie został znaleziony."));
        workout.markDeleted();
        workouts.save(workout);
        return new ApiResponse("Trening został usunięty.");
    }

    @DeleteMapping("/plan")
    @Operation(summary = "Anuluj aktywny plan", description = "Anuluje cały aktywny plan treningowy")
    public ApiResponse cancelPlan() {
        int count = generator.cancelActivePlan();
        return new ApiResponse(count == 0
                ? "Nie masz aktywnego planu treningowego."
                : "Anulowano plan: " + count + " treningów.");
    }

    @GetMapping("/{id}/download/zwo")
    @Operation(summary = "Pobierz plik ZWO", description = "Pobiera trening w formacie ZWO (Zwift)")
    public ResponseEntity<Resource> downloadZwo(@PathVariable Long id) {
        var workout = workouts.findById(id)
                .filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String filename = workout.getPlannedDate() + "_" + workout.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".zwo";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(workout.getZwoContent().getBytes(StandardCharsets.UTF_8)));
    }

    @GetMapping("/{id}/download/fit")
    @Operation(summary = "Pobierz plik FIT", description = "Pobiera trening w formacie FIT (Garmin)")
    public ResponseEntity<Resource> downloadFit(@PathVariable Long id) {
        var workout = workouts.findById(id)
                .filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String filename = workout.getPlannedDate() + "_" + workout.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".fit";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(fitService.encode(workout)));
    }
}
