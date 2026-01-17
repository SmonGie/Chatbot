package chatbot.backend.application.common.interfaces;

import chatbot.backend.application.knowledge.VectorDatabaseDocument;

import java.util.List;

public interface RerankerGateway {
    List<VectorDatabaseDocument> rerank(String query, List<VectorDatabaseDocument> documentsTemp);
}
