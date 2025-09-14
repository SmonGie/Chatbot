package chatbot.backend.domain.repositories;

import chatbot.backend.domain.entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IConversationRepository extends MongoRepository<Conversation,String>{ }
