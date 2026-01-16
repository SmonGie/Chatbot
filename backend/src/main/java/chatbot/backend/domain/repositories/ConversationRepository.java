package chatbot.backend.domain.repositories;
import chatbot.backend.domain.entities.Conversation;

import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(String id);
    Conversation save(Conversation conversation);
}