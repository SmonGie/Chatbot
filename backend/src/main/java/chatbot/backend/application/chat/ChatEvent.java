package chatbot.backend.application.chat;

public sealed interface ChatEvent
        permits BotMessageChunk, BotFinalMessage, FollowupsEvent, ErrorEvent, ConversationStart, FollowupsErrorEvent {
}
