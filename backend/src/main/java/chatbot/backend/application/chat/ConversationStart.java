package chatbot.backend.application.chat;

public record ConversationStart(String conversationId) implements ChatEvent {
}
