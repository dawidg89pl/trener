package pl.trenerai.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import pl.trenerai.repository.AppUserRepository;

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


