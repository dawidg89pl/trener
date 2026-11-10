package pl.trenerai.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AiProviderCatalogTest {

    @Test
    void providersListIsNotEmpty() {
        assertFalse(AiProviderCatalog.providers().isEmpty());
    }

    @Test
    void modelsListIsNotEmpty() {
        assertFalse(AiProviderCatalog.models().isEmpty());
    }

    @Test
    void providerByIdReturnsCorrectProvider() {
        var openai = AiProviderCatalog.provider("openai");
        assertEquals("openai", openai.id());
        assertEquals("OpenAI", openai.label());
        assertNotNull(openai.chatCompletionsUrl());
    }

    @Test
    void providerByInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AiProviderCatalog.provider("nonexistent"));
    }

    @Test
    void isSupportedReturnsTrueForValidCombination() {
        assertTrue(AiProviderCatalog.isSupported("openai", "gpt-4o"));
    }

    @Test
    void isSupportedReturnsFalseForInvalidCombination() {
        assertFalse(AiProviderCatalog.isSupported("openai", "nonexistent-model"));
    }

    @Test
    void isSupportedReturnsFalseForMismatchedProviderModel() {
        assertFalse(AiProviderCatalog.isSupported("groq", "gpt-4o"));
    }

    @Test
    void allModelsReferenceExistingProvider() {
        var providerIds = AiProviderCatalog.providers().stream()
                .map(AiProviderCatalog.Provider::id).toList();
        for (var model : AiProviderCatalog.models()) {
            assertTrue(providerIds.contains(model.providerId()),
                    "Model " + model.id() + " references unknown provider " + model.providerId());
        }
    }
}
