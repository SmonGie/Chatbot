package chatbot.backend.domain.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
public class FollowUpQuestion {
    @Id
    private String id;
    private String messageId;
    private String content;

    FollowUpQuestion(String messageId, String content) {
        id = UUID.randomUUID().toString();
        this.messageId = messageId;
        this.content = content;
    }
}
