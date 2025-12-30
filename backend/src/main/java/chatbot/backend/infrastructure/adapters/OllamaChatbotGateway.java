package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.ChatbotGateway;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
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
        String instruction =
            """
            Na podstawie tej odpowiedzi wygeneruj maksymalnie 7 follow-up questions, bez pomijania kluczowych słów, które student Politechniki Łódzkiej mógłby zadać jako następne.
            Zwróć tylko listę pytań w formacie JSON (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.
            Trzymaj się tematu rozmowy.
            Pytania muszą dotyczyć Politechniki Łódzkiej.

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

    @Override
    public String rewrite(String query, String context) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(
                    """
                    Jesteś botem do normalizacji zapytań użytkownika.
                    Nie jesteś chatbotem i nie prowadzisz rozmowy.
                
                    Twoje zadanie:
                    Przepisz zapytanie użytkownika na jedno, jednoznaczne pytanie.
                
                     Reguły:
                     - NIE odpowiadaj na pytanie
                     - NIE komentuj
                     - NIE zadawaj żadnych pytań
                     - NIE dodawaj nowych informacji
                     - NIE usuwaj istotnych informacji
                     - NIE zmieniaj intencji zapytania
                     - NIE używaj zwrotów grzecznościowych
                     - NIE używaj cudzysłowów ani formatowania
                
                     Dozwolone:
                     - Doprecyzowanie zapytania wyłącznie poprzez dodanie brakujących słów kluczowych
                     - Usunięcie niejednoznaczności zapytania
                
                     Format odpowiedzi:
                     Zwróć wyłącznie przepisane pytanie jako jeden ciąg tekstu.
                """),
                new UserMessage(
                    """
                    [KONTEKST DIALOGOWY]:
                    %s

                    [PYTANIE UŻYTKOWNIKA]:
                    %s
                """.formatted(context, query))
        ));

        return Objects.requireNonNull(chatModel.call(prompt)
                        .getResult()
                        .getOutput()
                        .getText())
                .trim();
    }
}