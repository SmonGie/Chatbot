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

@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;
    private final List<Message> messages;
    private final Instant createdAt;

    Conversation() {
        this.id = UUID.randomUUID().toString();
        createdAt = Instant.now();
        messages = new ArrayList<>();
    }

    public List<Message> getMessages() {
        return List.copyOf(messages);
    }

    public List<Message> getLastMessages(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Message count cannot be negative"
            );
        }

        int size = messages.size();
        return List.copyOf(
                messages.subList(Math.max(0, size - amount), size)
        );
    }
}
