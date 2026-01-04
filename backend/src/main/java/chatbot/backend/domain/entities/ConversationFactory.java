package chatbot.backend.domain.entities;

public class ConversationFactory {
    private ConversationFactory() {}

    public static Conversation createConversation() {
        return new Conversation();
    }
}
