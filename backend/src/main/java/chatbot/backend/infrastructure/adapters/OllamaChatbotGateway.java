package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.common.interfaces.ChatbotGateway;
import chatbot.backend.domain.enums.TemplatesFollowup;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OllamaChatbotGateway implements ChatbotGateway {
    private final OllamaChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Autowired
    public OllamaChatbotGateway(OllamaChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<String> response(Prompt prompt) {
        return chatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText());
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
                         - Jeśli pytanie nie miało sensu lub używało niecenzuralnych słów napisz: "Pytanie użytkownika było niecenzuralne i zostało usunięte"
                    
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

    @Override
    public Flux<String> followupsWithPromptEngineering(String answer) {
        String instruction =
            """
            Na podstawie poniższej odpowiedzi wygeneruj maksymalnie 7 nowych pytań follow-up, bez pomijania kluczowych słów, które student Politechniki Łódzkiej mógłby zadać jako następne.
           
            Reguły:
                - Trzymaj się tematu rozmowy.
                - Pytania muszą dotyczyć Politechniki Łódzkiej.
                - Logicznie rozwijaj temat rozmowy.

            Odpowiedź:
            %s
           
            Zwróć tylko listę pytań w formacie JSON
            (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.
           """.formatted(answer);

        return generateResponse(instruction);
    }

    @Override
    public Flux<String> followupsWithRAG(List<String> similarQuestions) {
        String joinedQuestions = similarQuestions.stream()
                .map(q -> "- " + q)
                .collect(Collectors.joining("\n"));

        String instruction =
                """
                Na podstawie poniższych pytań wygeneruj maksymalnie 7 nowych pytań follow-up.
                
                Reguły:
                    - Pytania mają rozwijać temat rozmowy.
                    - Pytania dotyczą Politechniki Łódzkiej.
                    - Nie powtarzaj podanych pytań, ale logicznie je rozwijaj.
                
                Podobne pytania:
                %s
    
                Zwróć tylko listę pytań w formacie JSON
                (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.
                """.formatted(joinedQuestions);

        return generateResponse(instruction);
    }

    @Override
    public Flux<String> followupsWithTemplates(String category) {
        TemplatesFollowup templateCategory =
                TemplatesFollowup.fromCategory(category);

        try {
            String json = objectMapper.writeValueAsString(
                    templateCategory.getTemplates()
                            .stream()
                            .limit(8)
                            .toList()
            );
            return Flux.just(json);
        } catch (Exception e) {
            return Flux.just("[]");
        }
    }

    @NonNull
    private Flux<String> generateResponse(String instruction) {
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