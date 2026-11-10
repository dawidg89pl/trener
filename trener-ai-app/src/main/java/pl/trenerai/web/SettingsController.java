package pl.trenerai.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.trenerai.model.UserSettings;
import pl.trenerai.repository.UserSettingsRepository;
import pl.trenerai.service.AiProviderCatalog;
import pl.trenerai.service.CurrentUserService;
import pl.trenerai.service.SecretCrypto;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final CurrentUserService current;
    private final UserSettingsRepository settings;
    private final SecretCrypto crypto;

    public SettingsController(CurrentUserService current, UserSettingsRepository settings, SecretCrypto crypto) {
        this.current = current;
        this.settings = settings;
        this.crypto = crypto;
    }

    @GetMapping
    String page(Model model) {
        model.addAttribute("settings", current.settings());
        model.addAttribute("aiProviders", AiProviderCatalog.providers());
        model.addAttribute("aiModels", AiProviderCatalog.models());
        return "settings";
    }

    @PostMapping
    String save(@RequestParam String openAiKey,
                @RequestParam String aiProvider,
                @RequestParam String openAiModel) {

        if (!AiProviderCatalog.isSupported(aiProvider, openAiModel)) {
            throw new IllegalArgumentException("Nieobsługiwane połączenie dostawcy i modelu AI.");
        }

        UserSettings set = current.settings();

        if (!set.getAiProvider().equals(aiProvider) && openAiKey.isBlank()) {
            throw new IllegalArgumentException("Po zmianie dostawcy wklej jego klucz API.");
        }

        if (!openAiKey.isBlank()) {
            set.setOpenAiKeyEncrypted(crypto.encrypt(openAiKey));
        }
        set.setAiProvider(aiProvider);
        set.setOpenAiModel(openAiModel);
        settings.save(set);

        return "redirect:/settings?saved";
    }
}
