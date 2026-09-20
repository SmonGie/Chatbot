package chatbot.backend.domain.entities;

import chatbot.backend.domain.enums.Sender;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Message {
    @Id
    private String id;
    private final String conversationId;
    private final Sender sender;
    private final String content;
    private final Instant timestamp;

    Message(String conversationId, Sender sender, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be empty");
        }
        if (sender == null) {
            throw new IllegalArgumentException("Message sender cannot be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        this.id = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
        this.timestamp = Instant.now();
    }
}
