package chatbot.backend.domain.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter

@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;
    private List<Message> messages;
    private Instant createdAt;
    private Instant endedAt;

    Conversation() {
        this.id = UUID.randomUUID().toString();
        createdAt = Instant.now();
        messages = new ArrayList<>();
    }

    public void sendMessage(Message message){
        if (endedAt != null) {
            throw new IllegalStateException("Conversation is already ended.");
        }
        this.messages.add(message);
    }

    public void end() {
        if (endedAt == null) {
            this.endedAt = Instant.now();
        }
    }
}
