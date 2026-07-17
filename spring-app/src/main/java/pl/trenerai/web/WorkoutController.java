package pl.trenerai.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.service.*;

@Controller
public class WorkoutController {
    private final WorkoutGenerationService generator;
    private final CurrentUserService current;
    private final GeneratedWorkoutRepository workouts;
    private final UserEventService eventLog;

    public WorkoutController(WorkoutGenerationService generator, CurrentUserService current,
            GeneratedWorkoutRepository workouts, UserEventService eventLog) {
        this.generator = generator;
        this.current = current;
        this.workouts = workouts;
        this.eventLog = eventLog;
    }

    @PostMapping("/workouts/generate")
    String generate(@RequestParam(required = false) List<Long> activityIds, @RequestParam LocalDate startDate,
            RedirectAttributes redirect) {
        if (activityIds == null || activityIds.isEmpty()) {
            redirect.addFlashAttribute("error", "Zaznacz co najmniej jedną aktywność przed wygenerowaniem treningów.");
            return "redirect:/";
        }
        try {
            int count = generator.generate(activityIds, startDate);
            redirect.addFlashAttribute("message", "Wygenerowano " + count + " pliki ZWO.");
        } catch (Exception error) {
            redirect.addFlashAttribute("error",
                    error.getMessage() == null ? "Nie udało się wygenerować treningów." : error.getMessage());
        }
        return "redirect:/";
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

    @PostMapping("/workouts/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes redirect) {
        var workout = workouts.findById(id).filter(w -> !w.isDeleted() && w.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
        workout.markDeleted();
        workouts.save(workout);
        eventLog.record(current.get(), "WORKOUT_DELETED", "Usunięto trening „" + workout.getName() + "”");
        redirect.addFlashAttribute("message", "Trening został usunięty z listy.");
        return "redirect:/";
    }
}
