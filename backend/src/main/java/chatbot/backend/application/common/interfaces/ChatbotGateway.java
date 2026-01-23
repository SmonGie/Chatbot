package chatbot.backend.application.common.interfaces;

import chatbot.backend.application.chat.PromptMessage;
import chatbot.backend.domain.entities.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatbotGateway {
    Flux<String> response(PromptMessage prompt);
    Flux<String> followupsWithRAG(List<String> similarQuestions, List<Message> history, String answer);
    String rewrite(String query, List<Message> context);
    Flux<String> followupsWithPromptEngineering(String answer, List<Message> history);
    Flux<String> followupsWithTemplates(String category);
}