package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.RerankerGateway;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
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
    public List<VectorDatabaseDocument> rerank(String query, List<VectorDatabaseDocument> documentsTemp) {

        if (documentsTemp.isEmpty()) {
            return documentsTemp;
        }

        return documentsTemp.stream()
                .map(doc -> Map.entry(
                        doc,
                        reranker.score(query, doc.question()).content()
                ))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }
}
