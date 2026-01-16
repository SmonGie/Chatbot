package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.VectorDatabaseSearchGateway;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VectorDatabaseSearchAdapter implements VectorDatabaseSearchGateway {

    private final VectorStore vectorStore;

    public VectorDatabaseSearchAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<VectorDatabaseDocument> findSimilarDocuments(String query, int limit) {
        return vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(limit)
                                .similarityThreshold(0.5)
                                .build()
                ).stream()
                .map(document -> new VectorDatabaseDocument(document.getText(), String.valueOf(document.getMetadata().get("pytanie")), String.valueOf(document.getMetadata().get("typ"))))
                .toList();
    }
}
