package chatbot.backend.Application.Services;

import chatbot.backend.Domain.Entities.Conversation;
import chatbot.backend.Domain.Entities.Message;
import chatbot.backend.Domain.Repositories.IConversationRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final IConversationRepository conversationRepository;

    public ConversationService(IConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation startConversation(){
        Conversation conversation = new Conversation();
        conversation.setCreatedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation sendMessage(String conversationId, Message message){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();

        conversation.sendMessage(message);

        return conversationRepository.save(conversation);
    }
}
