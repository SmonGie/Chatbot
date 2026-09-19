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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConversationService {
    private static final int TARGET_CHUNK_SIZE = 40;
    private static final int MAX_BUFFER_SIZE = 80;

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
        message.setConversationId(conversationId);

        boolean found = conversationRepository.appendMessage(conversationId, message);

        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
    }

    public Conversation getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }

    public Flux<ChatEvent> streamBotResponse(String conversationId, String query, FollowupMethod method, Degree level) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean firstChunkSent = new AtomicBoolean(false);
        AtomicLong firstChunkTime = new AtomicLong(0);
        AtomicInteger chunkCount = new AtomicInteger(0);
        Message userMessage = MessageFactory.createUserMessage(query);
        userMessage.setConversationId(conversationId);
        sendMessage(conversationId, userMessage);

        Conversation conversation = getConversationById(conversationId);

        List<Message> lastMessages = new ArrayList<>(conversation.getLastMessages(4));
        String fixedContent = chatbotGateway.rewrite(query, lastMessages);

        List<VectorDatabaseDocument> similarDocs = vectorDatabaseSearchGateway.findSimilarDocuments(fixedContent, 30, level);
        List<VectorDatabaseDocument> reranked = rerankerGateway.rerank(fixedContent, similarDocs);
        List<VectorDatabaseDocument> faqsContext = reranked.stream().limit(5).toList();
        PromptMessage prompt = new PromptMessage(fixedContent, lastMessages ,faqsContext);

        StringBuilder responseBuilder = new StringBuilder();
        StringBuilder buffer = new StringBuilder();

        return chatbotGateway.response(prompt)
                .flatMap(chunk -> {
                    buffer.append(chunk);

                    List<ChatEvent> events = splitChunks(buffer, responseBuilder);

                    for (ChatEvent event : events) {
                        if (event instanceof BotMessageChunk) {

                            chunkCount.incrementAndGet();

                            if (!firstChunkSent.get()) {
                                firstChunkSent.set(true);
                                long ttft = System.currentTimeMillis() - startTime;
                                firstChunkTime.set(ttft);

                                log.info("TTFT (time to first chunk): {} ms", ttft);
                            }
                        }
                    }

                    return Flux.fromIterable(events);
                })
                .concatWith(Flux.defer(() -> {
                    Flux<ChatEvent> finalText = Flux.empty();

                    if (!buffer.isEmpty()) {
                        String remainingData = buffer.toString();
                        buffer.setLength(0);
                        responseBuilder.append(remainingData);

                        finalText = Flux.just(new BotMessageChunk(remainingData));
                    }

                    String answer = responseBuilder.toString();
                    Message botMessage = MessageFactory.createBotMessage(answer);

                    Mono<Void> saveAnswer = Mono.fromRunnable(() -> {
                        sendMessage(conversationId, botMessage);
                    }).subscribeOn(Schedulers.boundedElastic()).then();


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

                    Flux<ChatEvent> followups = Flux.defer(() ->generateFollowups(method, answer, similarQuestions, category, lastMessages))
                            .map(json -> {
                                try {
                                    return objectMapper.readValue(json, new TypeReference<List<String>>() {
                                    });
                                } catch (JsonProcessingException e) {
                                    throw new IllegalStateException(
                                            "Invalid JSON of followup questions",
                                            e
                                    );
                                }
                            })
                            .map(list -> (ChatEvent) new FollowupsEvent(list))
                            .onErrorResume(error -> {
                                log.warn(
                                        "Followups could not be generated for conversation {}",
                                        conversationId,
                                        error
                                );

                                return Flux.just(new FollowupsErrorEvent(
                                        "Additional followups were not generated."
                                ));
                            });

                    return Flux.concat(finalText, saveAnswer.thenMany(followups));
                }))
                .publishOn(Schedulers.boundedElastic())
                .doFinally(_ -> {
                    long totalTime = System.currentTimeMillis() - startTime;

                    log.info("Total latency: {} ms", totalTime);
                    log.info("Chunk count: {}", chunkCount.get());

                    if (firstChunkTime.get() > 0) {
                        long streamingTime = totalTime - firstChunkTime.get();
                        log.info("Streaming duration: {} ms", streamingTime);
                    }
                })
                .onErrorResume(err -> Flux.just(new ErrorEvent(err.getMessage())));
    }

    private List<ChatEvent> splitChunks(StringBuilder buffer, StringBuilder responseBuilder) {
        List<ChatEvent> events = new ArrayList<>();
        Optional<String> nextChunk = takeReadyChunk(buffer);

        while (nextChunk.isPresent()) {
            String text = nextChunk.get();
            responseBuilder.append(text);
            events.add(new BotMessageChunk(text));
            nextChunk = takeReadyChunk(buffer);
        }

        return events;
    }

    private Optional<String> takeReadyChunk(StringBuilder buffer) {
        if (buffer.length() < TARGET_CHUNK_SIZE) {
            return Optional.empty();
        }

        int splitIndex = findLastSeparator(buffer);

        if (splitIndex < TARGET_CHUNK_SIZE && buffer.length() < MAX_BUFFER_SIZE) {
            return Optional.empty();
        }

        if (splitIndex <= 0) {
            String chunk = buffer.toString();
            buffer.setLength(0);
            return Optional.of(chunk);
        }

        String chunk = buffer.substring(0, splitIndex);
        buffer.delete(0, splitIndex);
        return Optional.of(chunk);
    }

    private int findLastSeparator(StringBuilder buffer) {
        for (int index = buffer.length() - 1; index >= 0; index--) {
            if (isSeparator(buffer.charAt(index))) {
                return index + 1;
            }
        }

        return -1;
    }

    private boolean isSeparator(char character) {
        return Character.isWhitespace(character) || ",.;:!?)]}\"".indexOf(character) >= 0;
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
