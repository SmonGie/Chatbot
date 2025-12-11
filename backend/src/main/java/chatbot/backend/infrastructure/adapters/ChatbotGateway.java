package chatbot.backend.infrastructure.adapters;

import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public interface ChatbotGateway {
    Flux<String> response(Prompt prompt);
    Flux<String> followups(String answer);
}