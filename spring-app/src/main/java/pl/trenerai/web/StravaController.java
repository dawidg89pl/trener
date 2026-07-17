package pl.trenerai.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.trenerai.service.StravaService;

@Controller
@RequestMapping("/strava")
public class StravaController {
    private final StravaService strava;

    public StravaController(StravaService strava) {
        this.strava = strava;
    }

    @GetMapping("/connect")
    String connect(RedirectAttributes redirect) {
        try {
            return "redirect:" + strava.authorizationUrl();
        } catch (IllegalStateException error) {
            redirect.addFlashAttribute("error", error.getMessage());
            return "redirect:/settings";
        }
    }

    @GetMapping("/callback")
    String callback(@RequestParam(required = false) String code, RedirectAttributes redirect) {
        try {
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("Autoryzacja Stravy została anulowana.");
            }
            strava.callback(code);
            redirect.addFlashAttribute("message", "Konto Strava zostało połączone.");
        } catch (Exception error) {
            redirect.addFlashAttribute("error", error.getMessage() == null
                    ? "Nie udało się połączyć ze Stravą." : error.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/sync")
    String sync(RedirectAttributes redirect) {
        try {
            var result = strava.syncActivities();
            redirect.addFlashAttribute("message", "Zsynchronizowano " + result.activities()
                    + " aktywności (pełne szczegóły: " + result.detailed()
                    + ", moc: " + result.withPower()
                    + ", tętno: " + result.withHeartRate()
                    + ", kadencja: " + result.withCadence() + ").");
        } catch (IllegalStateException error) {
            redirect.addFlashAttribute("error", error.getMessage());
        } catch (Exception error) {
            redirect.addFlashAttribute("error", "Nie udało się pobrać aktywności ze Stravy.");
        }
        return "redirect:/";
    }
}
