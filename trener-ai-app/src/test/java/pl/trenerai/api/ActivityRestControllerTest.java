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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityRestControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserBootstrapService bootstrap;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "api-activity-" + System.nanoTime() + "@test.pl";
        bootstrap.createIfMissing(testEmail, "password");
    }

    @Test
    void listActivitiesReturnsJson() throws Exception {
        mvc.perform(get("/api/activities").with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listActivitiesRequiresAuth() throws Exception {
        mvc.perform(get("/api/activities"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getActivityNotFoundReturns404() throws Exception {
        mvc.perform(get("/api/activities/999999").with(user(testEmail)))
                .andExpect(status().isNotFound());
    }
}
