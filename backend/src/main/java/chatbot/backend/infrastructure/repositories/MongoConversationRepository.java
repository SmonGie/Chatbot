package chatbot.backend.infrastructure.repositories;
import chatbot.backend.domain.entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoConversationRepository extends MongoRepository<Conversation, String> { }