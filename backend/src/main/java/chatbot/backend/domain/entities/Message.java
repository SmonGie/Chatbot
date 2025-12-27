package chatbot.backend.domain.entities;

import chatbot.backend.domain.enums.Sender;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Message {
    @Id
    private String id;
    private String conversationId;
    private Sender sender;
    private String content;
    private Instant timestamp;

    Message(Sender sender, String content) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.content = content;
        this.timestamp = Instant.now();
    }
}
