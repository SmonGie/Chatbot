package chatbot.backend.infrastructure.adapters;

import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.entities.Message;
import chatbot.backend.domain.repositories.ConversationRepository;
import chatbot.backend.infrastructure.repositories.MongoConversationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final MongoConversationRepository conversationRepo;
    private final MongoTemplate mongoTemplate;

    public ConversationRepositoryAdapter(MongoConversationRepository repo, MongoTemplate mongoTemplate) {
        this.conversationRepo = repo;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Conversation> findById(String id) {
        return conversationRepo.findById(id);
    }

    @Override
    public Conversation save(Conversation conversation) {
        return conversationRepo.save(conversation);
    }

    @Override
    public boolean appendMessage(String conversationId, Message message) {
        var result = mongoTemplate.update(Conversation.class)
                .matching(Criteria.where("id").is(conversationId))
                .apply(new Update().push("messages").value(message))
                .first();

        return result.getMatchedCount() > 0;
    }
}