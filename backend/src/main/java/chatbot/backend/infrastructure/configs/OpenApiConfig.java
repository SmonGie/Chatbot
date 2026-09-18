package chatbot.backend.infrastructure.configs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Chatbot API",
                version = "1.0",
                description = "API for interacting with the chatbot"
        )
)
public class OpenApiConfig {
}
