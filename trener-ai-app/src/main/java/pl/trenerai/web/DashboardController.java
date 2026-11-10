package pl.trenerai.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.trenerai.repository.ActivityRepository;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.repository.StravaTokenRepository;
import pl.trenerai.repository.UserEventRepository;
import pl.trenerai.repository.UserSettingsRepository;
import pl.trenerai.model.Activity;
import pl.trenerai.model.GeneratedWorkout;
import pl.trenerai.service.CurrentUserService;
import pl.trenerai.service.WorkoutSummaryService;

@Controller
public class DashboardController {
    private final CurrentUserService current;
    private final ActivityRepository activities;
    private final GeneratedWorkoutRepository workouts;
    private final UserEventRepository events;
    private final StravaTokenRepository stravaTokens;
    private final UserSettingsRepository settings;
    private final WorkoutSummaryService workoutSummaries;
    private static final DateTimeFormatter WEEK_LABEL =
            DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pl-PL"));
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("pl-PL"));

    public DashboardController(
            CurrentUserService current,
            ActivityRepository activities,
            GeneratedWorkoutRepository workouts,
            UserEventRepository events,
            StravaTokenRepository stravaTokens,
            UserSettingsRepository settings,
            WorkoutSummaryService workoutSummaries) {
        this.current = current;
        this.activities = activities;
        this.workouts = workouts;
        this.events = events;
        this.stravaTokens = stravaTokens;
        this.settings = settings;
        this.workoutSummaries = workoutSummaries;
    }

    @GetMapping("/")
    String dashboard(@RequestParam(required = false) String week, Model model) {
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
        LocalDate weekStartDate = parseWeek(week);
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        Map<LocalDate, List<Activity>> activitiesByDate = activityList.stream()
                .filter(activity -> !activity.getActivityDate().isBefore(weekStartDate)
                        && !activity.getActivityDate().isAfter(weekEndDate))
                .collect(Collectors.groupingBy(Activity::getActivityDate));
        Map<LocalDate, List<GeneratedWorkout>> workoutsByDate = generatedWorkouts.stream()
                .filter(workout -> !workout.getPlannedDate().isBefore(weekStartDate)
                        && !workout.getPlannedDate().isAfter(weekEndDate))
                .collect(Collectors.groupingBy(GeneratedWorkout::getPlannedDate));
        List<WeekDay> weekDays = java.util.stream.IntStream.range(0, 7)
                .mapToObj(offset -> {
                    LocalDate date = weekStartDate.plusDays(offset);
                    return new WeekDay(date, DAY_LABEL.format(date).replace(".", ""),
                            date.equals(LocalDate.now()),
                            activitiesByDate.getOrDefault(date, List.of()),
                            workoutsByDate.getOrDefault(date, List.of()));
                }).toList();
        Map<Long, WorkoutSummaryService.WorkoutSummary> summariesByWorkout = generatedWorkouts.stream()
                .collect(Collectors.toMap(GeneratedWorkout::getId, workoutSummaries::summarize));

        model.addAttribute("user", user);
        model.addAttribute("activities", activityList);
        model.addAttribute("weeklySummary", weeklySummary);
        model.addAttribute("stravaFtp", userSettings.getStravaFtp());
        model.addAttribute("stravaConnected", stravaTokens.findById(user.getId())
                .map(token -> token.getStravaAthleteId() != null)
                .orElse(false));
        model.addAttribute("workoutCount", generatedWorkouts.size());
        model.addAttribute("planActive", !generatedWorkouts.isEmpty());
        model.addAttribute("recentEvents", events.findByUserIdOrderByCreatedAtDesc(user.getId()));
        model.addAttribute("defaultStartDate", LocalDate.now().plusDays(1));
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("weekLabel", WEEK_LABEL.format(weekStartDate) + " – "
                + WEEK_LABEL.format(weekEndDate));
        model.addAttribute("previousWeek", weekStartDate.minusWeeks(1));
        model.addAttribute("nextWeek", weekStartDate.plusWeeks(1));
        model.addAttribute("workoutSummaries", summariesByWorkout);
        return "dashboard";
    }

    private LocalDate parseWeek(String value) {
        LocalDate selected;
        try {
            selected = value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
        } catch (Exception ignored) {
            selected = LocalDate.now();
        }
        return selected.minusDays(selected.getDayOfWeek().getValue() - 1L);
    }

    @PostMapping("/ftp")
    String saveFtp(@RequestParam Integer ftp, RedirectAttributes redirect) {
        if (ftp < 1 || ftp > 2000) {
            redirect.addFlashAttribute("error", "FTP musi mieścić się w zakresie od 1 do 2000 W.");
            return "redirect:/#generator";
        }
        var userSettings = current.settings();
        userSettings.setStravaFtp(ftp);
        settings.save(userSettings);
        redirect.addFlashAttribute("message", "FTP zostało zapisane ręcznie.");
        return "redirect:/#generator";
    }

    public record WeeklySummary(int count, double distanceKm, String duration, double elevationM) {}
    public record WeekDay(LocalDate date, String label, boolean today,
            List<Activity> activities, List<GeneratedWorkout> workouts) {}
}
