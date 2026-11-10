package pl.trenerai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Trener AI API")
                .version("1.0.0")
                .description("REST API aplikacji Trener AI — inteligentny asystent planowania treningów kolarskich. "
                        + "Umożliwia zarządzanie aktywnościami ze Stravy, generowanie planów treningowych "
                        + "z użyciem AI oraz eksport treningów do formatów ZWO i FIT.")
                .contact(new Contact().name("Trener AI")));
    }
}
