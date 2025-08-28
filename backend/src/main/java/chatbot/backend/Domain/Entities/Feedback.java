package chatbot.backend.Domain.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Feedback {
    private String id;
    private String messageId;
    private int rating;
    private String comment; // Opcjonalny
}
