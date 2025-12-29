package chatbot.backend.application.common.interfaces;

import org.springframework.ai.document.Document;

import java.util.List;

public interface RerankerGateway {
    List<Document> rerank(String query, List<Document> documents);
}
