package pl.trenerai.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.repository.StravaTokenRepository;
import pl.trenerai.service.*;

@Controller
public class WorkoutController {
    private final WorkoutGenerationService generator;
    private final CurrentUserService current;
    private final GeneratedWorkoutRepository workouts;
    private final UserEventService eventLog;
    private final FitWorkoutService fitWorkoutService;
    private final StravaTokenRepository stravaTokens;

    public WorkoutController(WorkoutGenerationService generator, CurrentUserService current,
            GeneratedWorkoutRepository workouts, UserEventService eventLog,
            FitWorkoutService fitWorkoutService, StravaTokenRepository stravaTokens) {
        this.generator = generator;
        this.current = current;
        this.workouts = workouts;
        this.eventLog = eventLog;
        this.fitWorkoutService = fitWorkoutService;
        this.stravaTokens = stravaTokens;
    }

    @PostMapping("/workouts/generate")
    String generate(@RequestParam(required = false) List<Long> activityIds, @RequestParam LocalDate startDate,
            @RequestParam String trainingGoal, @RequestParam BigDecimal weeklyTrainingHours,
            RedirectAttributes redirect) {
        if (stravaTokens.findById(current.get().getId())
                .map(token -> token.getStravaAthleteId() == null)
                .orElse(true)) {
            redirect.addFlashAttribute("error", "Najpierw połącz konto Strava.");
            return "redirect:/#generator";
        }
        if (activityIds == null || activityIds.isEmpty()) {
            redirect.addFlashAttribute("error", "Zaznacz co najmniej jedną aktywność przed wygenerowaniem treningów.");
            return "redirect:/#generator";
        }
        try {
            int count = generator.generate(activityIds, startDate,
                    WorkoutGenerationService.TrainingGoal.fromId(trainingGoal), weeklyTrainingHours);
            redirect.addFlashAttribute("message", "Twój 7-dniowy plan jest aktywny. AI zaplanowało " + count + " treningów, a pozostałe dni przeznaczyło na regenerację.");
            return "redirect:/?week=" + startDate + "#calendar";
        } catch (Exception error) {
            redirect.addFlashAttribute("error",
                    error.getMessage() == null ? "Nie udało się wygenerować treningów." : error.getMessage());
            return "redirect:/#generator";
        }
    }

    @GetMapping("/workouts/{id}/download")
    ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        var workout = workouts.findById(id).filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
        String file = workout.getPlannedDate() + "_" + workout.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".zwo";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(new ByteArrayResource(workout.getZwoContent().getBytes(StandardCharsets.UTF_8)));
    }

    @GetMapping("/workouts/{id}/download-fit")
    ResponseEntity<ByteArrayResource> downloadFit(@PathVariable Long id) {
        var workout = workouts.findById(id)
                .filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
        String file = workout.getPlannedDate() + "_"
                + workout.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".fit";
        byte[] content = fitWorkoutService.encode(workout);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(new ByteArrayResource(content));
    }

    @PostMapping("/workouts/{id}/delete")
    String delete(@PathVariable Long id, @RequestParam(required = false) String returnTo,
            @RequestParam(required = false) String returnMonth,
            RedirectAttributes redirect) {
        var workout = workouts.findById(id).filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
        workout.markDeleted();
        workouts.save(workout);
        eventLog.record(current.get(), "WORKOUT_DELETED", "Usunięto trening „" + workout.getName() + "”");
        redirect.addFlashAttribute("message", "Trening został usunięty z listy.");
        if ("/calendar".equals(returnTo)) {
            return "redirect:/#calendar";
        }
        return "redirect:/#plan-status";
    }

    @PostMapping("/workouts/plan/cancel")
    String cancelPlan(RedirectAttributes redirect) {
        int count = generator.cancelActivePlan();
        redirect.addFlashAttribute("message", count == 0
                ? "Nie masz aktywnego planu treningowego."
                : "Plan został anulowany. Możesz teraz wygenerować nowy.");
        return "redirect:/";
    }
}
