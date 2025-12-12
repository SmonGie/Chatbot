package chatbot.backend.application.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class VectorLoader {
    @Autowired
    private VectorStore vectorStore;

    @PostConstruct
    public void loadFAQs() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/faq.json")) {
            List<Map<String, String>> faqs = mapper.readValue(is, new TypeReference<>() {});
            List<Document> documents = faqs.stream()
                    .map(f -> new Document(f.get("answer"), Map.of("question", f.get("question"))))
                    .toList();
            vectorStore.add(documents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
