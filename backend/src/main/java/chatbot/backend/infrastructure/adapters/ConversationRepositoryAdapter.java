package chatbot.backend.infrastructure.adapters;

import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.repositories.ConversationRepository;
import chatbot.backend.infrastructure.repositories.MongoConversationRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final MongoConversationRepository conversationRepo;

    public ConversationRepositoryAdapter(MongoConversationRepository repo) {
        this.conversationRepo = repo;
    }

    @Override
    public Optional<Conversation> findById(String id) {
        return conversationRepo.findById(id);
    }

    @Override
    public Conversation save(Conversation conversation) {
        return conversationRepo.save(conversation);
    }
}