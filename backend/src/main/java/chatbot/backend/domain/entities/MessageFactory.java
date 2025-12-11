package chatbot.backend.domain.entities;

import chatbot.backend.domain.enums.Sender;

public class MessageFactory {
    private MessageFactory() {}

    public static Message createUserMessage(String content) {
        validate(content);
        return new Message(Sender.USER, content);
    }

    public static Message createBotMessage(String content) {
        validate(content);
        return new Message(Sender.BOT, content);
    }

    private static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
    }
}
