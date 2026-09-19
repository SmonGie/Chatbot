package chatbot.backend.domain.repositories;
import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.entities.Message;

import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(String id);
    Conversation save(Conversation conversation);
    boolean appendMessage(String conversationId, Message message);
}