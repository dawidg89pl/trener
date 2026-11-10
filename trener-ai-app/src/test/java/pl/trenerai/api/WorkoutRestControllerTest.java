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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkoutRestControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserBootstrapService bootstrap;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "api-workout-" + System.nanoTime() + "@test.pl";
        bootstrap.createIfMissing(testEmail, "password");
    }

    @Test
    void listWorkoutsReturnsEmptyArray() throws Exception {
        mvc.perform(get("/api/workouts").with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getWorkoutNotFoundReturns404() throws Exception {
        mvc.perform(get("/api/workouts/999999").with(user(testEmail)))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateWithoutBodyReturns400() throws Exception {
        mvc.perform(post("/api/workouts/generate")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelPlanWhenNoneActive() throws Exception {
        mvc.perform(delete("/api/workouts/plan").with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nie masz aktywnego planu treningowego."));
    }

    @Test
    void deleteWorkoutNotFoundReturns404() throws Exception {
        mvc.perform(delete("/api/workouts/999999").with(user(testEmail)))
                .andExpect(status().isNotFound());
    }
}
