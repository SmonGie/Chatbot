package chatbot.backend.domain.entities;

import chatbot.backend.domain.enums.Sender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Message {
    @Id
    private String id;
    private String conversationId;
    private Sender sender;
    private String content;
    private Instant timestamp;

    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public Message(Sender sender, String content) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.content = content;
        this.timestamp = Instant.now();
    }

}
