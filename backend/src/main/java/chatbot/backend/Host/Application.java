package chatbot.backend.Host;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@SpringBootApplication(scanBasePackages = {
        "chatbot.backend.Domain",
        "chatbot.backend.Application",
        "chatbot.backend.Infrastructure",
        "chatbot.backend.Host"
})
@EnableMongoRepositories(basePackages = "chatbot.backend.Domain.Repositories")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
