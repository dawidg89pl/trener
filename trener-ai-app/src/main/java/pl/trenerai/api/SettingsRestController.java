package pl.trenerai.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.trenerai.repository.UserSettingsRepository;
import pl.trenerai.service.AiProviderCatalog;
import pl.trenerai.service.CurrentUserService;
import pl.trenerai.service.SecretCrypto;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Ustawienia", description = "Konfiguracja użytkownika i dostawcy AI")
public class SettingsRestController {

    private final CurrentUserService current;
    private final UserSettingsRepository settings;
    private final SecretCrypto crypto;

    public SettingsRestController(CurrentUserService current, UserSettingsRepository settings, SecretCrypto crypto) {
        this.current = current;
        this.settings = settings;
        this.crypto = crypto;
    }

    @GetMapping
    @Operation(summary = "Pobierz ustawienia", description = "Zwraca aktualne ustawienia użytkownika")
    public SettingsDto get() {
        var s = current.settings();
        return new SettingsDto(s.getAiProvider(), s.getOpenAiModel(), s.getStravaFtp(),
                s.getOpenAiKeyEncrypted() != null && !s.getOpenAiKeyEncrypted().isBlank());
    }

    @PutMapping("/ai")
    @Operation(summary = "Zmień ustawienia AI",
            description = "Aktualizuje dostawcę AI, model i opcjonalnie klucz API")
    public ApiResponse updateAi(@Valid @RequestBody AiSettingsRequest request) {
        if (!AiProviderCatalog.isSupported(request.aiProvider(), request.aiModel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieobsługiwane połączenie dostawcy i modelu AI.");
        }
        var s = current.settings();
        if (!s.getAiProvider().equals(request.aiProvider()) && (request.apiKey() == null || request.apiKey().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Po zmianie dostawcy podaj jego klucz API.");
        }
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            s.setOpenAiKeyEncrypted(crypto.encrypt(request.apiKey()));
        }
        s.setAiProvider(request.aiProvider());
        s.setOpenAiModel(request.aiModel());
        settings.save(s);
        return new ApiResponse("Ustawienia AI zostały zapisane.");
    }

    @PutMapping("/ftp")
    @Operation(summary = "Ustaw FTP", description = "Zapisuje wartość FTP (Functional Threshold Power)")
    public ApiResponse updateFtp(@Valid @RequestBody FtpRequest request) {
        var s = current.settings();
        s.setStravaFtp(request.ftp());
        settings.save(s);
        return new ApiResponse("FTP zostało zapisane: " + request.ftp() + " W.");
    }

    @GetMapping("/ai/providers")
    @Operation(summary = "Lista dostawców AI", description = "Zwraca dostępnych dostawców AI i ich modele")
    public ProvidersDto providers() {
        return new ProvidersDto(AiProviderCatalog.providers().stream()
                .map(p -> new ProviderDto(p.id(), p.label(), p.apiKeyUrl())).toList(),
                AiProviderCatalog.models().stream()
                        .map(m -> new ModelDto(m.providerId(), m.id(), m.label())).toList());
    }

    @Schema(description = "Aktualne ustawienia użytkownika")
    public record SettingsDto(
            @Schema(description = "ID dostawcy AI", example = "openai") String aiProvider,
            @Schema(description = "ID modelu AI", example = "gpt-4o") String aiModel,
            @Schema(description = "FTP w watach", example = "250") Integer ftp,
            @Schema(description = "Czy klucz API jest skonfigurowany") boolean apiKeyConfigured) {}

    @Schema(description = "Żądanie zmiany ustawień AI")
    public record AiSettingsRequest(
            @NotBlank @Schema(description = "ID dostawcy AI", example = "openai") String aiProvider,
            @NotBlank @Schema(description = "ID modelu AI", example = "gpt-4o") String aiModel,
            @Schema(description = "Klucz API (opcjonalny jeśli dostawca się nie zmienia)") String apiKey) {}

    @Schema(description = "Żądanie ustawienia FTP")
    public record FtpRequest(
            @Min(value = 1, message = "FTP musi wynosić co najmniej 1 W.")
            @Max(value = 2000, message = "FTP nie może przekraczać 2000 W.")
            @Schema(description = "Wartość FTP w watach", example = "250")
            int ftp) {}

    @Schema(description = "Dostępni dostawcy AI i modele")
    public record ProvidersDto(List<ProviderDto> providers, List<ModelDto> models) {}

    @Schema(description = "Dostawca AI")
    public record ProviderDto(
            @Schema(example = "openai") String id,
            @Schema(example = "OpenAI") String label,
            @Schema(description = "URL do uzyskania klucza API") String apiKeyUrl) {}

    @Schema(description = "Model AI")
    public record ModelDto(
            @Schema(example = "openai") String providerId,
            @Schema(example = "gpt-4o") String id,
            @Schema(example = "GPT-4o — dotychczasowy model") String label) {}
}
