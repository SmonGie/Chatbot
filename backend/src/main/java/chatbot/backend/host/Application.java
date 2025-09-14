package chatbot.backend.host;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@SpringBootApplication(scanBasePackages = {
        "chatbot.backend.domain",
        "chatbot.backend.application",
        "chatbot.backend.infrastructure",
        "chatbot.backend.host"
})
@EnableMongoRepositories(basePackages = "chatbot.backend.domain.repositories")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
