package chatbot.backend.Host;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {
        "chatbot.backend.Domain",
        "chatbot.backend.Application",
        "chatbot.backend.Infrastructure"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
