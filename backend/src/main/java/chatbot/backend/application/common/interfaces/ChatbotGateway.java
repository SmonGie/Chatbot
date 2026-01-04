package chatbot.backend.application.common.interfaces;

import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatbotGateway {
    Flux<String> response(Prompt prompt);
    Flux<String> followupsWithRAG(List<String> answer);
    String rewrite(String query, String context);
    Flux<String> followupsWithPromptEngineering(String similarQuestions);
    Flux<String> followupsWithTemplates(String category);
}