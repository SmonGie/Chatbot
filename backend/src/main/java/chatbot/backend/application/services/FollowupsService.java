package chatbot.backend.application.services;

import chatbot.backend.application.common.interfaces.ChatbotGateway;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import chatbot.backend.domain.entities.Message;
import chatbot.backend.domain.enums.FollowupMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FollowupsService {
    private final ObjectMapper objectMapper;
    private final ChatbotGateway chatbotGateway;
    private final StringBuilder buffer = new StringBuilder();
    private final StringBuilder responseBuilder = new StringBuilder();

    @Autowired
    public FollowupsService(ObjectMapper objectMapper, ChatbotGateway chatbotGateway){
        this.objectMapper = objectMapper;
        this.chatbotGateway = chatbotGateway;
    }
    public Mono<List<String>> generateFollowups(FollowupMethod method, String answer, List<VectorDatabaseDocument> context, List<Message> history){
        List<String> similarQuestions = context.stream()
                .map(VectorDatabaseDocument::question)
                .toList();
        String category = context.stream()
                .map(VectorDatabaseDocument::category)
                .collect(Collectors.groupingBy(typ -> typ, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("ogolne");

        Mono<String> response = switch (method) {
            case PROMPT_ENGINEERING ->
                    chatbotGateway.followupsWithPromptEngineering(answer, history);
            case RAG ->
                    chatbotGateway.followupsWithRAG(similarQuestions, history, answer);
            case TEMPLATE_BASED ->
                    chatbotGateway.followupsWithTemplates(category);
        };

        return response.map(json -> {
            try {
                return objectMapper.readValue(json, new TypeReference<>(){});
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Niepoprawny JSON pytań follow-up",
                        e
                );
            }
        });
    }
}
