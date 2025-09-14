package chatbot.backend.application.services;

import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.entities.Message;
import chatbot.backend.domain.repositories.IConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final IConversationRepository conversationRepository;

    @Autowired
    public ConversationService(IConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation startConversation(){
        Conversation conversation = new Conversation();
        conversation.setCreatedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation endConversation(String conversationId){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        conversation.setEndedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation sendMessage(String conversationId, Message message){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        message.setConversationId(conversationId);
        conversation.sendMessage(message);
        return conversationRepository.save(conversation);
    }

    public Conversation getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }

}
