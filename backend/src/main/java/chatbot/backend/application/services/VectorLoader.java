package chatbot.backend.application.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class VectorLoader {
    private final VectorStore vectorStore;

    public VectorLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadFAQs() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/faq.json")) {
            List<Map<String, String>> faqs = mapper.readValue(is, new TypeReference<>() {});
            List<Document> documents = faqs.stream()
                    .map(faq -> new Document(faq.get("odpowiedz"), Map.of("pytanie", faq.get("pytanie"),
                            "typ", faq.get("typ"))))
                    .toList();
            vectorStore.add(documents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
