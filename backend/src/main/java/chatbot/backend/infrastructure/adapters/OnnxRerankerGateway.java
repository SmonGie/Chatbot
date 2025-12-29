package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.RerankerGateway;
import org.springframework.ai.document.Document;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OnnxRerankerGateway implements RerankerGateway {
    private final OnnxScoringModel reranker;

    public OnnxRerankerGateway() {
        this.reranker = new OnnxScoringModel(
                "backend/src/main/resources/models/model_quint8_avx2.onnx",
                "backend/src/main/resources/models/tokenizer.json"
        );
    }

    @Override
    public List<Document> rerank(String query, List<Document> documents) {

        if (documents.isEmpty()) {
            return documents;
        }

        return documents.stream()
                .map(doc -> Map.entry(
                        doc,
                        reranker.score(query,  doc.getMetadata().get("pytanie").toString()).content()
                ))
                .sorted((docA, docB) -> Double.compare(docB.getValue(), docA.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }
}
