package chatbot.backend.application.chat;

import java.util.List;

public record FollowupsEvent(List<String> followups) implements ChatEvent {
}
