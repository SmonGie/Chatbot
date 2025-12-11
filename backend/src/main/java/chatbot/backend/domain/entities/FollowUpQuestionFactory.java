package chatbot.backend.domain.entities;

public class FollowUpQuestionFactory {
    public static FollowUpQuestion create(String messageId, String content) {
        return new FollowUpQuestion(messageId, content);
    }
}
