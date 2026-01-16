package chatbot.backend.application.chat;

public record BotMessageChunk(String text) implements ChatEvent {
}
