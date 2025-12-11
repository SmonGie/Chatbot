package chatbot.backend.infrastructure.adapters;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Component
public class OllamaChatbotGateway implements ChatbotGateway {
    private final OllamaChatModel chatModel;

    @Autowired
    public OllamaChatbotGateway(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Flux<String> response(Prompt prompt) {
        return chatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText());
    }

    @Override
    public Flux<String> followups(String answer) {
        String instruction = """
            Na podstawie tej odpowiedzi wygeneruj maksymalnie 6 follow-up questions.
            Zwróć tylko listę pytań w formacie JSON (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.

            Odpowiedź:
            %s
            """.formatted(answer);

        return chatModel.stream(new Prompt(instruction))
                .map(chatResponse -> Objects.requireNonNull(chatResponse.getResult().getOutput().getText()))
                .reduce("", (result, chunk) -> result + chunk)
                .map(text -> {
                    text = text.trim();
                    int start = text.indexOf('[');
                    int end = text.lastIndexOf(']') + 1;
                    if (start >= 0 && end > start) {
                        return text.substring(start, end);
                    }
                    return "[]";
                })
                .flux()
                .onErrorReturn("[]");
    }
}