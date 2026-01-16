package chatbot.backend.host;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@Slf4j
@SpringBootApplication(scanBasePackages = {
        "chatbot.backend.domain",
        "chatbot.backend.application",
        "chatbot.backend.infrastructure",
        "chatbot.backend.host"
})
@EnableMongoRepositories(basePackages = "chatbot.backend.infrastructure.repositories")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
