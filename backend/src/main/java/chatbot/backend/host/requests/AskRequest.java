package chatbot.backend.host.requests;

import chatbot.backend.application.enums.Degree;
import chatbot.backend.domain.enums.FollowupMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank(message = "Content must not be blank")
        @Size(max = 2000, message = "Content must not exceed 2000 characters")
        String content,
        String conversationId,
        FollowupMethod method,
        Degree level
) {
    public AskRequest {
        method = method == null ? FollowupMethod.RAG : method;
        level = level == null ? Degree.all : level;
    }
}
