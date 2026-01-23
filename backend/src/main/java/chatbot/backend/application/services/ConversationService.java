package chatbot.backend.application.services;

import chatbot.backend.application.chat.*;
import chatbot.backend.application.common.interfaces.RerankerGateway;
import chatbot.backend.application.common.interfaces.VectorDatabaseSearchGateway;
import chatbot.backend.application.enums.Degree;
import chatbot.backend.application.knowledge.VectorDatabaseDocument;
import chatbot.backend.domain.entities.*;
import chatbot.backend.domain.enums.FollowupMethod;
import chatbot.backend.domain.repositories.ConversationRepository;
import chatbot.backend.application.common.interfaces.ChatbotGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ChatbotGateway chatbotGateway;
    private final RerankerGateway rerankerGateway;
    private final ObjectMapper objectMapper;
    private final VectorDatabaseSearchGateway vectorDatabaseSearchGateway;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository, ChatbotGateway chatbotGateway, RerankerGateway rerankerGateway, ObjectMapper objectMapper, VectorDatabaseSearchGateway vectorDatabaseSearchGateway) {
        this.conversationRepository = conversationRepository;
        this.chatbotGateway = chatbotGateway;
        this.rerankerGateway = rerankerGateway;
        this.objectMapper = objectMapper;
        this.vectorDatabaseSearchGateway = vectorDatabaseSearchGateway;
    }

    public Conversation startConversation(){
        Conversation conversation = ConversationFactory.createConversation();
        conversation.setCreatedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public void sendMessage(String conversationId, Message message){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        message.setConversationId(conversationId);
        conversation.sendMessage(message);
        conversationRepository.save(conversation);
    }

    public Conversation getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }

    public Flux<ChatEvent> streamBotResponse(String conversationId, String query, FollowupMethod method, Degree level) {
        Message userMessage = MessageFactory.createUserMessage(query);
        userMessage.setConversationId(conversationId);
        sendMessage(conversationId, userMessage);

        Conversation conversation = getConversationById(conversationId);

        List<Message> lastMessages = new ArrayList<>(conversation.getLastMessages(4));
        String fixedContent = chatbotGateway.rewrite(query, lastMessages);

        List<VectorDatabaseDocument> similarDocs = vectorDatabaseSearchGateway.findSimilarDocuments(fixedContent, 5, level);
        List<VectorDatabaseDocument> reranked = rerankerGateway.rerank(fixedContent, similarDocs);
        List<VectorDatabaseDocument> faqsContext = reranked.stream().limit(5).toList();
        PromptMessage prompt = new PromptMessage(fixedContent, lastMessages ,faqsContext);

        StringBuilder responseBuilder = new StringBuilder();
        StringBuilder buffer = new StringBuilder();

        return chatbotGateway.response(prompt)
                .flatMap(chunk -> {
                    buffer.append(chunk);

                    if (buffer.length() >= 120) {
                        String toSend = buffer.toString();
                        buffer.setLength(0);
                        responseBuilder.append(toSend);

                        return Flux.<ChatEvent>just(new BotMessageChunk(toSend));
                    }

                    return Flux.empty();
                })
                .concatWith(Flux.defer(() -> {
                    Flux<ChatEvent> finalText = Flux.empty();

                    if (!buffer.isEmpty()) {
                        String remainingData = buffer.toString();
                        buffer.setLength(0);
                        responseBuilder.append(remainingData);

                        finalText = Flux.just(new BotMessageChunk(remainingData));
                    }

                    Message botMessage = MessageFactory.createBotMessage(responseBuilder.toString());
                    botMessage.setConversationId(conversationId);
                    conversation.sendMessage(botMessage);

                    String answer = responseBuilder.toString();

                    List<String> similarQuestions = faqsContext.stream()
                            .map(VectorDatabaseDocument::question)
                            .toList();

                    String category = faqsContext.stream()
                            .map(VectorDatabaseDocument::category)
                            .collect(Collectors.groupingBy(typ -> typ, Collectors.counting()))
                                    .entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse("ogolne");

                    Flux<ChatEvent> followups = generateFollowups(method, answer, similarQuestions, category, lastMessages)
                            .map(this::parseFollowupsJson)
                            .map(list -> (ChatEvent) new FollowupsEvent(list))
                            .onErrorReturn(new FollowupsEvent(List.of()));

                    return Flux.concat(finalText, followups);
                }))
                .publishOn(Schedulers.boundedElastic())
                .doFinally(_ -> conversationRepository.save(conversation))
                .onErrorResume(err -> Flux.just(new ErrorEvent(err.getMessage())));
    }

    private List<String> parseFollowupsJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public Flux<ChatEvent> startAndStreamBotResponse(String userContent, FollowupMethod method, Degree level) {
        Conversation conversation = startConversation();
        ChatEvent convId = new ConversationStart(conversation.getId());
        return Flux.concat(Flux.just(convId), streamBotResponse(conversation.getId(), userContent, method, level));
    }

    public Flux<String> generateFollowups(
            FollowupMethod method,
            String answer,
            List<String> similarQuestions,
            String category,
            List<Message> history)
    {
        return switch (method) {
            case PROMPT_ENGINEERING -> chatbotGateway.followupsWithPromptEngineering(answer, history);
            case RAG -> chatbotGateway.followupsWithRAG(similarQuestions, history, answer);
            case TEMPLATE_BASED -> chatbotGateway.followupsWithTemplates(category);
        };
    }
}
