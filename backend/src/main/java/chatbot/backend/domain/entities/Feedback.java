package chatbot.backend.domain.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter

@Document(collection = "feedbacks")
public class Feedback {
    @Id
    private String id;
    private String messageId;
    private int rating;
    private String content;
}
