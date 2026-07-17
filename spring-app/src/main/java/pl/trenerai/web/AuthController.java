package pl.trenerai.web;

import jakarta.validation.constraints.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pl.trenerai.repository.*;
import pl.trenerai.service.UserBootstrapService;

@Controller
public class AuthController {
    private final AppUserRepository users;
    private final UserBootstrapService bootstrap;

    public AuthController(AppUserRepository users, UserBootstrapService bootstrap) {
        this.users = users;
        this.bootstrap = bootstrap;
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/register")
    String register(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    String register(@ModelAttribute("form") RegistrationForm form, BindingResult errors) {
        if (users.findByEmailIgnoreCase(form.email()).isPresent())
            errors.rejectValue("email", "duplicate", "Ten adres e-mail jest już używany.");
        if (!form.password().equals(form.passwordConfirmation()))
            errors.rejectValue("passwordConfirmation", "mismatch", "Hasła muszą być takie same.");
        if (errors.hasErrors())
            return "register";
        bootstrap.createIfMissing(form.email(), form.password());
        return "redirect:/login?registered";
    }

    public record RegistrationForm(@Email @NotBlank String email,
            @Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków.") String password,
            String passwordConfirmation) {
        public RegistrationForm() {
            this("", "", "");
        }
    }
}
