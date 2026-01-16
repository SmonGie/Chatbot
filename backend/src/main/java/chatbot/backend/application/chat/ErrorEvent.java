package chatbot.backend.application.chat;

public record ErrorEvent(String text) implements ChatEvent {
}
