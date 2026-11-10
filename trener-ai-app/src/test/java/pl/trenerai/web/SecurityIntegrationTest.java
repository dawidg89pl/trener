package pl.trenerai.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPageIsPublic() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void cssIsPublic() throws Exception {
        mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk());
    }

    @Test
    void jsIsPublic() throws Exception {
        mvc.perform(get("/js/theme.js"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardRedirectsToLoginWhenUnauthenticated() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void settingsRedirectsToLoginWhenUnauthenticated() throws Exception {
        mvc.perform(get("/settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void faqRedirectsToLoginWhenUnauthenticated() throws Exception {
        mvc.perform(get("/faq"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
