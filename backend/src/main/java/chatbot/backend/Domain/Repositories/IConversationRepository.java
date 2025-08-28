package chatbot.backend.Domain.Repositories;

import chatbot.backend.Domain.Entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IConversationRepository extends MongoRepository<Conversation,String>{ }
