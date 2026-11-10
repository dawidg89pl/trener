package pl.trenerai.service;

import java.util.List;

public final class AiProviderCatalog {
    public record ModelOption(String providerId, String id, String label) {}
    public record Provider(String id, String label, String chatCompletionsUrl, String apiKeyUrl) {}

    private static final List<Provider> PROVIDERS = List.of(
            new Provider("openai", "OpenAI", "https://api.openai.com/v1/chat/completions", "https://platform.openai.com/api-keys"),
            new Provider("gemini", "Google Gemini — dostępny plan bezpłatny", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "https://aistudio.google.com/app/apikey"),
            new Provider("groq", "Groq — dostępny plan bezpłatny", "https://api.groq.com/openai/v1/chat/completions", "https://console.groq.com/keys")
    );

    private static final List<ModelOption> MODELS = List.of(
            new ModelOption("openai", "gpt-4.1", "GPT-4.1 — najwyższa jakość"),
            new ModelOption("openai", "gpt-4.1-mini", "GPT-4.1 mini — szybki i oszczędny"),
            new ModelOption("openai", "gpt-4o", "GPT-4o — dotychczasowy model"),
            new ModelOption("openai", "gpt-4o-mini", "GPT-4o mini — najniższy koszt"),
            new ModelOption("gemini", "gemini-3.5-flash", "Gemini 3.5 Flash"),
            new ModelOption("groq", "llama-3.3-70b-versatile", "Llama 3.3 70B — wyższa jakość"),
            new ModelOption("groq", "llama-3.1-8b-instant", "Llama 3.1 8B — szybki")
    );

    private AiProviderCatalog() {}
    public static List<Provider> providers() { return PROVIDERS; }
    public static List<ModelOption> models() { return MODELS; }
    public static boolean isSupported(String providerId, String modelId) { return MODELS.stream().anyMatch(model -> model.providerId().equals(providerId) && model.id().equals(modelId)); }
    public static Provider provider(String providerId) { return PROVIDERS.stream().filter(provider -> provider.id().equals(providerId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Nieobsługiwany dostawca AI.")); }
}
