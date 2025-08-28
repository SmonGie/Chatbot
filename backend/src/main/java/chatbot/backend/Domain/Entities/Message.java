package chatbot.backend.Domain.Entities;

import chatbot.backend.Domain.Enums.Sender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String id;
    private String conversationId;
    private Sender sender;
    private String content;
    private Instant timestamp;
}
