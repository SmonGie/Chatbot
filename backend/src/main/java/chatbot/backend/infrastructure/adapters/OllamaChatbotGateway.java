package chatbot.backend.infrastructure.adapters;

import chatbot.backend.application.chat.PromptMessage;
import chatbot.backend.application.common.interfaces.ChatbotGateway;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import chatbot.backend.domain.entities.Message;
import chatbot.backend.domain.enums.Sender;
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
    public Flux<String> response(PromptMessage promptTemp) {
        Prompt prompt = buildPrompt(promptTemp);
        return chatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText());
    }

    @Override
    public String rewrite(String query, List<Message> history) {
        String context = formatHistory(history);

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

        return Objects.requireNonNull(chatModel.call(prompt).getResult().getOutput().getText()).trim();
    }

    @Override
    public Flux<String> followupsWithPromptEngineering(String answer, List<Message> history) {
        String messageHistory = formatHistory(history);

        String instruction =
            """
            Na podstawie poniższej odpowiedzi i kontekstu dialogowego wygeneruj maksymalnie 7 nowych pytań follow-up, bez pomijania kluczowych słów, które student Politechniki Łódzkiej mógłby zadać jako następne.
           
            Reguły:
                - Pytania mają rozwijać temat rozmowy.
                - Pytania muszą dotyczyć Politechniki Łódzkiej.
                - Nie powtarzaj podanych pytań, ale logicznie je rozwijaj.
            
            Kontekst dialogowy:
            %s

            Odpowiedź:
            %s
           
            Zwróć tylko listę pytań w formacie JSON
            (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.
           """.formatted(messageHistory, answer);

        return generateResponse(instruction);
    }

    @Override
    public Flux<String> followupsWithRAG(List<String> similarQuestions, List<Message> history) {
        String joinedQuestions = similarQuestions.stream()
                .map(q -> "- " + q)
                .collect(Collectors.joining("\n"));

        String messageHistory = formatHistory(history);

        String instruction =
                """
                Na podstawie poniższych pytań i kontekstu dialogowego wygeneruj maksymalnie 7 nowych pytań follow-up.
                
                Reguły:
                    - Pytania mają rozwijać temat rozmowy.
                    - Pytania muszą dotyczyć Politechniki Łódzkiej.
                    - Nie powtarzaj podanych pytań, ale logicznie je rozwijaj.
                
                Kontekst dialogowy:
                %s
                
                Podobne pytania:
                %s
    
                Zwróć tylko listę pytań w formacie JSON
                (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.
                """.formatted(messageHistory, joinedQuestions);

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

    private Prompt buildPrompt(PromptMessage promptTemp) {
        String systemPrompt =
                """
                Jesteś Tulbotem, chatbotem odpowiadającym wyłącznie na pytania o Politechnice Łódzkiej.
        
                Ścisłe reguły:
                 - Odpowiadaj WYŁĄCZNIE na podstawie przekazanego kontekstu.
                 - NIE korzystaj z wiedzy spoza kontekstu i niczego nie dopowiadaj.
                 - Odpowiedzi formułuj grzecznie i konkretnie.
                 - NIE cytuj pytania użytkownika.
                 - Jeżeli w kontekście nie ma żadnych dokumentów, które odpowiadają na pytanie, odpisz że nie jesteś w stanie odpowiedzieć na to pytanie, nawet jeśli jest ono bardzo proste.
                 - Jeżeli pytanie NIE dotyczy Politechniki Łódzkiej, poinformuj o tym uprzejmie i w sposób zrozumiały.
                 - ZAWSZE pozostawaj w roli Tulbota.
                """;

        String knowledge = formatKnowledge(promptTemp.knowledgeContext());
        String history   = formatHistory(promptTemp.history());

        String userPrompt =
                """
                [KONTEKST - JEDYNE ŹRÓDŁO WIEDZY]
                %s
        
                [KONTEKST DIALOGOWY - NIE JEST ŹRÓDŁEM WIEDZY]
                %s
        
                [AKTUALNE PYTANIE UŻYTKOWNIKA]
                %s
                """.formatted(knowledge, history, promptTemp.question());

        return new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));
    }

    private String formatKnowledge(List<VectorDatabaseDocument> docs) {
        if (docs.isEmpty()) {
            return "Brak dostępnych informacji w bazie wiedzy.";
        }

        return docs.stream()
                .map(document -> "[INFORMACJA]\n" + document.text())
                .collect(Collectors.joining("\n"));
    }

    private String formatHistory(List<Message> messages) {
        return messages.stream()
                .map(m -> m.getSender() == Sender.BOT
                        ? "BOT: " + m.getContent()
                        : "UŻYTKOWNIK: " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

}