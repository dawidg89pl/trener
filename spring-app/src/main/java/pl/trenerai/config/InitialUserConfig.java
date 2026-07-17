package pl.trenerai.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import pl.trenerai.service.UserBootstrapService;

@Configuration
public class InitialUserConfig {
    @Bean
    @ConditionalOnProperty(name = "app.seed-user.enabled", havingValue = "true")
    CommandLineRunner seedUser(
            UserBootstrapService bootstrap,
            @Value("${app.seed-user.email}") String email,
            @Value("${app.seed-user.password}") String password) {
        return args -> bootstrap.createIfMissing(email, password);
    }
}
