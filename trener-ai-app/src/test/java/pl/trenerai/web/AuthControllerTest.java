package pl.trenerai.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pl.trenerai.repository.AppUserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Test
    void registerCreatesUserAndRedirects() throws Exception {
        String email = "newuser-" + System.nanoTime() + "@test.pl";

        mvc.perform(post("/register")
                        .param("email", email)
                        .param("password", "password123")
                        .param("passwordConfirmation", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertTrue(users.findByEmailIgnoreCase(email).isPresent());
    }

    @Test
    void registerWithMismatchedPasswordsShowsError() throws Exception {
        mvc.perform(post("/register")
                        .param("email", "mismatch@test.pl")
                        .param("password", "password123")
                        .param("passwordConfirmation", "different456")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerWithDuplicateEmailShowsError() throws Exception {
        String email = "duplicate-" + System.nanoTime() + "@test.pl";

        mvc.perform(post("/register")
                        .param("email", email)
                        .param("password", "password123")
                        .param("passwordConfirmation", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/register")
                        .param("email", email)
                        .param("password", "password123")
                        .param("passwordConfirmation", "password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }
}
