package pl.trenerai.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pl.trenerai.service.UserBootstrapService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserBootstrapService bootstrap;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "dashboard-" + System.nanoTime() + "@test.pl";
        bootstrap.createIfMissing(testEmail, "password");
    }

    @Test
    void dashboardReturnsOkForAuthenticatedUser() throws Exception {
        mvc.perform(get("/").with(user(testEmail).password("password").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user", "activities", "weeklySummary", "weekDays"));
    }

    @Test
    void dashboardWithWeekParam() throws Exception {
        mvc.perform(get("/?week=2026-11-02").with(user(testEmail).password("password").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void dashboardWithInvalidWeekParamFallsBackToCurrentWeek() throws Exception {
        mvc.perform(get("/?week=invalid").with(user(testEmail).password("password").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void saveFtpValidValue() throws Exception {
        mvc.perform(post("/ftp")
                        .param("ftp", "250")
                        .with(user(testEmail).password("password").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#generator"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    void saveFtpTooHighRejectsWithError() throws Exception {
        mvc.perform(post("/ftp")
                        .param("ftp", "3000")
                        .with(user(testEmail).password("password").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#generator"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void saveFtpTooLowRejectsWithError() throws Exception {
        mvc.perform(post("/ftp")
                        .param("ftp", "0")
                        .with(user(testEmail).password("password").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#generator"))
                .andExpect(flash().attributeExists("error"));
    }
}
