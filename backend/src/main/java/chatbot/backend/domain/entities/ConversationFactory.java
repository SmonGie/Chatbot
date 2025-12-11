package chatbot.backend.domain.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class ConversationFactory {
    private ConversationFactory() {}

    public static Conversation createConversation() {
        return new Conversation();
    }
}
