package chatbot.backend.application.chat;

public record FollowupsErrorEvent(String errorMessage) implements ChatEvent {}
