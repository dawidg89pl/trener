package pl.trenerai.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.trenerai.service.UserBootstrapService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsRestControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserBootstrapService bootstrap;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "api-settings-" + System.nanoTime() + "@test.pl";
        bootstrap.createIfMissing(testEmail, "password");
    }

    @Test
    void getSettingsReturnsDefaults() throws Exception {
        mvc.perform(get("/api/settings").with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiProvider").value("openai"))
                .andExpect(jsonPath("$.aiModel").value("gpt-4o"))
                .andExpect(jsonPath("$.apiKeyConfigured").value(false));
    }

    @Test
    void updateFtpValidValue() throws Exception {
        mvc.perform(put("/api/settings/ftp")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ftp\": 260}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FTP zostało zapisane: 260 W."));

        mvc.perform(get("/api/settings").with(user(testEmail)))
                .andExpect(jsonPath("$.ftp").value(260));
    }

    @Test
    void updateFtpInvalidValue() throws Exception {
        mvc.perform(put("/api/settings/ftp")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ftp\": 5000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProvidersReturnsNonEmptyLists() throws Exception {
        mvc.perform(get("/api/settings/ai/providers").with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers").isNotEmpty())
                .andExpect(jsonPath("$.models").isArray())
                .andExpect(jsonPath("$.models").isNotEmpty());
    }

    @Test
    void updateAiWithInvalidProviderModelReturns400() throws Exception {
        mvc.perform(put("/api/settings/ai")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aiProvider\": \"openai\", \"aiModel\": \"nonexistent\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void providersEndpointIsPublicWhenAuthenticated() throws Exception {
        mvc.perform(get("/api/settings/ai/providers").with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].id").exists());
    }
}
