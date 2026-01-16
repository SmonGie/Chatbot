package chatbot.backend.application.chat;

import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import chatbot.backend.domain.entities.Message;

import java.util.List;

public record PromptMessage(
        String question,
        List<Message> history,
        List<VectorDatabaseDocument> knowledgeContext
) {}
