package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.RerankerGateway;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Component
public class OnnxRerankerGateway implements RerankerGateway {
    private final OnnxScoringModel reranker;

    public OnnxRerankerGateway() {
        try {
            Path modelPath = copyResourceToTempFile("models/model_quint8_avx2.onnx", ".onnx");
            Path tokenizerPath = copyResourceToTempFile("models/tokenizer.json", ".json");

            this.reranker = new OnnxScoringModel(
                    modelPath.toString(),
                    tokenizerPath.toString()
            );
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się załadować modelu rerankera ONNX", e);
        }
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

    private Path copyResourceToTempFile(String resourcePath, String suffix) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);

        Path tempFile = Files.createTempFile("reranker-", suffix);

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        tempFile.toFile().deleteOnExit();
        return tempFile;
    }
}
