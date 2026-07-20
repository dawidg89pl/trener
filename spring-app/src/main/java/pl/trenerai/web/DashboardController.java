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
import pl.trenerai.repository.UserEventRepository;
import pl.trenerai.service.CurrentUserService;
import pl.trenerai.service.WorkoutSummaryService;

@Controller
public class DashboardController {
    private final CurrentUserService current;
    private final ActivityRepository activities;
    private final GeneratedWorkoutRepository workouts;
    private final UserEventRepository events;
    private final WorkoutSummaryService workoutSummaryService;

    public DashboardController(
            CurrentUserService current,
            ActivityRepository activities,
            GeneratedWorkoutRepository workouts,
            UserEventRepository events,
            WorkoutSummaryService workoutSummaryService) {
        this.current = current;
        this.activities = activities;
        this.workouts = workouts;
        this.events = events;
        this.workoutSummaryService = workoutSummaryService;
    }

    @GetMapping("/")
    String dashboard(Model model) {
        var user = current.get();
        var userSettings = current.settings();
        var activityList = activities.findByUserIdOrderByActivityDateDesc(user.getId());
        var weekStart = LocalDate.now().minusDays(6);
        var weeklyActivities = activityList.stream()
                .filter(activity -> !activity.getActivityDate().isBefore(weekStart))
                .toList();
        int weeklyMinutes = weeklyActivities.stream().mapToInt(activity -> activity.getMovingTimeMin()).sum();
        var weeklySummary = new WeeklySummary(
                weeklyActivities.size(),
                weeklyActivities.stream().mapToDouble(activity -> activity.getDistanceKm()).sum(),
                weeklyMinutes / 60 + " h " + weeklyMinutes % 60 + " min",
                weeklyActivities.stream().mapToDouble(activity -> activity.getElevationGainM()).sum());
        var generatedWorkouts = workouts.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(user.getId());
        var workoutBlocks = generatedWorkouts.stream()
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
        var workoutSummaries = generatedWorkouts.stream().collect(Collectors.toMap(
                GeneratedWorkout::getId,
                workoutSummaryService::summarize));

        model.addAttribute("user", user);
        model.addAttribute("activities", activityList);
        model.addAttribute("weeklySummary", weeklySummary);
        model.addAttribute("stravaFtp", userSettings.getStravaFtp());
        model.addAttribute("workoutBlocks", workoutBlocks);
        model.addAttribute("workoutSummaries", workoutSummaries);
        model.addAttribute("recentEvents", events.findByUserIdOrderByCreatedAtDesc(user.getId()));
        model.addAttribute("defaultStartDate", LocalDate.now().plusDays(1));
        return "dashboard";
    }

    public record WeeklySummary(int count, double distanceKm, String duration, double elevationM) {}
}
