package pl.trenerai.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.trenerai.model.GeneratedWorkout;
import pl.trenerai.repository.GeneratedWorkoutRepository;
import pl.trenerai.service.CurrentUserService;
import pl.trenerai.service.WorkoutSummaryService;

@Controller
public class CalendarController {
    private static final DateTimeFormatter MONTH_PARAMETER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL"));

    private final CurrentUserService current;
    private final GeneratedWorkoutRepository workouts;
    private final WorkoutSummaryService summaries;

    public CalendarController(CurrentUserService current, GeneratedWorkoutRepository workouts,
            WorkoutSummaryService summaries) {
        this.current = current;
        this.workouts = workouts;
        this.summaries = summaries;
    }

    @GetMapping("/calendar")
    String calendar(@RequestParam(required = false) String month, Model model) {
        YearMonth selectedMonth = parseMonth(month);
        List<GeneratedWorkout> allWorkouts = workouts
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(current.get().getId());
        Map<LocalDate, List<GeneratedWorkout>> workoutsByDate = allWorkouts.stream()
                .collect(Collectors.groupingBy(GeneratedWorkout::getPlannedDate));

        LocalDate gridStart = selectedMonth.atDay(1)
                .minusDays(selectedMonth.atDay(1).getDayOfWeek().getValue() - 1L);
        List<CalendarDay> days = new ArrayList<>();
        for (int offset = 0; offset < 42; offset++) {
            LocalDate date = gridStart.plusDays(offset);
            days.add(new CalendarDay(date, date.getMonth() == selectedMonth.getMonth(),
                    date.equals(LocalDate.now()), workoutsByDate.getOrDefault(date, List.of())));
        }

        Map<Long, WorkoutSummaryService.WorkoutSummary> workoutSummaries = allWorkouts.stream()
                .collect(Collectors.toMap(GeneratedWorkout::getId, summaries::summarize));
        String label = selectedMonth.format(MONTH_LABEL);
        model.addAttribute("monthLabel", Character.toUpperCase(label.charAt(0)) + label.substring(1));
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1).format(MONTH_PARAMETER));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1).format(MONTH_PARAMETER));
        model.addAttribute("calendarMonth", selectedMonth.format(MONTH_PARAMETER));
        model.addAttribute("days", days);
        model.addAttribute("workoutSummaries", workoutSummaries);
        return "calendar";
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) return YearMonth.now();
        try { return YearMonth.parse(value, MONTH_PARAMETER); }
        catch (Exception ignored) { return YearMonth.now(); }
    }

    public record CalendarDay(LocalDate date, boolean currentMonth, boolean today,
            List<GeneratedWorkout> workouts) {}
}
