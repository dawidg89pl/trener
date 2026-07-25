package pl.trenerai.web;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
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
    private static final String OAUTH_STATE_SESSION_KEY = "stravaOAuthState";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StravaService strava;

    public StravaController(StravaService strava) {
        this.strava = strava;
    }

    @GetMapping("/connect")
    String connect(HttpSession session, RedirectAttributes redirect) {
        try {
            String state = newState();
            session.setAttribute(OAUTH_STATE_SESSION_KEY, state);
            return "redirect:" + strava.authorizationUrl(state);
        } catch (IllegalStateException error) {
            redirect.addFlashAttribute("error", error.getMessage());
            return "redirect:/settings";
        }
    }

    @GetMapping("/callback")
    String callback(@RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session,
            RedirectAttributes redirect) {
        try {
            String expectedState = (String) session.getAttribute(OAUTH_STATE_SESSION_KEY);
            session.removeAttribute(OAUTH_STATE_SESSION_KEY);
            if (!validState(expectedState, state)) {
                throw new IllegalStateException("Nieprawidłowy stan autoryzacji Stravy. Spróbuj połączyć konto ponownie.");
            }
            if (error != null) {
                throw new IllegalStateException("Autoryzacja Stravy została anulowana.");
            }
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("Autoryzacja Stravy została anulowana.");
            }
            strava.callback(code);
            redirect.addFlashAttribute("message", "Konto Strava zostało połączone.");
        } catch (Exception error1) {
            redirect.addFlashAttribute("error", error1.getMessage() == null
                    ? "Nie udało się połączyć ze Stravą." : error1.getMessage());
        }
        return "redirect:/#generator";
    }

    private String newState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean validState(String expected, String received) {
        return expected != null && received != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
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
        return "redirect:/#generator";
    }
}
