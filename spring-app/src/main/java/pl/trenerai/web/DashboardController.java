package pl.trenerai.web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.trenerai.model.GeneratedWorkout;
import pl.trenerai.repository.ActivityRepository;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.service.CurrentUserService;

@Controller
public class DashboardController {
    private final CurrentUserService current;
    private final ActivityRepository activities;
    private final GeneratedWorkoutRepository workouts;

    public DashboardController(
            CurrentUserService current,
            ActivityRepository activities,
            GeneratedWorkoutRepository workouts) {
        this.current = current;
        this.activities = activities;
        this.workouts = workouts;
    }

    @GetMapping("/")
    String dashboard(Model model) {
        var user = current.get();
        var workoutBlocks = workouts.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(user.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        workout -> workout.getCreatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .truncatedTo(ChronoUnit.MINUTES),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), group -> {
                            group.sort(Comparator.comparing(GeneratedWorkout::getPlannedDate));
                            return group;
                        })));

        model.addAttribute("user", user);
        model.addAttribute("activities", activities.findByUserIdOrderByActivityDateDesc(user.getId()));
        model.addAttribute("workoutBlocks", workoutBlocks);
        model.addAttribute("defaultStartDate", LocalDate.now().plusDays(1));
        return "dashboard";
    }
}
