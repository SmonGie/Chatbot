package chatbot.backend.Domain.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;
    private List<Message> messages = new ArrayList<>();
    private Instant createdAt;
    private Instant endedAt;

    public void sendMessage(Message message){
        if (endedAt != null) {
            throw new IllegalStateException("Conversation has already ended.");
        }
        this.messages.add(message);
    }
}
