package pl.trenerai.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.trenerai.repository.ActivityRepository;
import pl.trenerai.service.CurrentUserService;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Aktywności", description = "Zarządzanie aktywnościami ze Stravy")
public class ActivityRestController {

    private final CurrentUserService current;
    private final ActivityRepository activities;

    public ActivityRestController(CurrentUserService current, ActivityRepository activities) {
        this.current = current;
        this.activities = activities;
    }

    @GetMapping
    @Operation(summary = "Lista aktywności", description = "Zwraca wszystkie aktywności użytkownika posortowane od najnowszej")
    public List<ActivityDto> list() {
        return activities.findByUserIdOrderByActivityDateDesc(current.get().getId())
                .stream().map(ActivityDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły aktywności", description = "Zwraca szczegóły pojedynczej aktywności")
    public ActivityDto get(@PathVariable Long id) {
        return activities.findById(id)
                .filter(a -> a.getUser().getId().equals(current.get().getId()))
                .map(ActivityDto::from)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Aktywność nie została znaleziona."));
    }
}
