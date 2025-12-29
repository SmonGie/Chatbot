package chatbot.backend.application.common.interfaces;

import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public interface ChatbotGateway {
    Flux<String> response(Prompt prompt);
    Flux<String> followups(String answer);
    String rewrite(String query, String context);
}