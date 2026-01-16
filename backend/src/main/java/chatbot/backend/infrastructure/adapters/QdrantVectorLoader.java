package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.VectorLoaderGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class QdrantVectorLoader implements VectorLoaderGateway {
    private final VectorStore vectorStore;
    private final ObjectMapper mapper = new ObjectMapper();

    public QdrantVectorLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    //@PostConstruct
    public void loadFAQs() {
        try (InputStream is = getClass().getResourceAsStream("/faq.json")) {
            List<Map<String, String>> faqs = mapper.readValue(is, new TypeReference<>() {});
            List<Document> documents = faqs.stream()
                    .map(faq -> new Document( faq.get("pytanie") + "\n" + faq.get("odpowiedz"), Map.of("pytanie", faq.get("pytanie"), "typ", faq.get("typ"))))
                    .toList();
            vectorStore.add(documents);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
