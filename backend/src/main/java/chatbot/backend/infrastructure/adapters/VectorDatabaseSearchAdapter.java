package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.VectorDatabaseSearchGateway;
import chatbot.backend.application.enums.Degree;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VectorDatabaseSearchAdapter implements VectorDatabaseSearchGateway {

    private final VectorStore vectorStore;

    public VectorDatabaseSearchAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<VectorDatabaseDocument> findSimilarDocuments(String query, int limit, Degree level) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(0.5);

        Filter.Expression filter = switch (level) {
            case I -> new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key("level"), new Filter.Value(List.of("I", "all"))
            );
            case II -> new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key("level"), new Filter.Value(List.of("II", "all"))
            );
            case all -> null;
        };

        if (filter != null) {
            builder.filterExpression(filter);
        }


        return vectorStore.similaritySearch(builder.build())
                .stream()
                .map(document -> new VectorDatabaseDocument(document.getText(), String.valueOf(document.getMetadata().get("pytanie")), String.valueOf(document.getMetadata().get("typ"))))
                .toList();
    }
}
