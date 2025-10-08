package chatbot.backend.host;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@Slf4j
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

    @Bean
    public WeaviateClient weaviateClient() {
        Config config = new Config("http", "localhost:8080");
        WeaviateClient client = new WeaviateClient(config);

        Result<Boolean> result = client.misc().readyChecker().run();
        log.info("✅ Weaviate ready: {}", result.getResult());

        return client;
    }

}
