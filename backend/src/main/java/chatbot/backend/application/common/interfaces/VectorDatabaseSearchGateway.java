package chatbot.backend.application.common.interfaces;

import chatbot.backend.application.knowledge.VectorDatabaseDocument;

import java.util.List;

public interface VectorDatabaseSearchGateway {
    List<VectorDatabaseDocument> findSimilarDocuments(String query, int target);
}
