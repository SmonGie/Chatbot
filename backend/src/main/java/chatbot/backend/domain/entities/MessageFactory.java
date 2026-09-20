package chatbot.backend.domain.entities;

import chatbot.backend.domain.enums.Sender;

public class MessageFactory {
    private MessageFactory() {}

    public static Message createUserMessage(String conversationId, String content) {
        return new Message(conversationId, Sender.USER, content);
    }

    public static Message createBotMessage(String conversationId, String content) {
        return new Message(conversationId, Sender.BOT, content);
    }
}
