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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ChatbotGateway chatbotGateway;
    private final RerankerGateway rerankerGateway;
    private final VectorDatabaseSearchGateway vectorDatabaseSearchGateway;
    private final FollowupsService followupsService;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository, ChatbotGateway chatbotGateway, RerankerGateway rerankerGateway, VectorDatabaseSearchGateway vectorDatabaseSearchGateway, FollowupsService followupsService) {
        this.conversationRepository = conversationRepository;
        this.chatbotGateway = chatbotGateway;
        this.rerankerGateway = rerankerGateway;
        this.vectorDatabaseSearchGateway = vectorDatabaseSearchGateway;
        this.followupsService = followupsService;
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
        return Flux.defer(() -> {
            ResponseChunkBuffer chunkBuffer = new ResponseChunkBuffer();
            return chatbotGateway.response(prompt)
                    .flatMap(chunk -> {
                        List<ChatEvent> events = chunkBuffer.append(chunk).stream()
                                .<ChatEvent>map(BotMessageChunk::new)
                                .toList();
                        return Flux.fromIterable(events);
                    })
                    .concatWith(Flux.defer(() -> completeResponse(
                            conversationId,
                            chunkBuffer,
                            method,
                            faqsContext,
                            lastMessages
                            )
                    ))
                    .doOnNext(event -> {
                        if (event instanceof BotMessageChunk) {
                            chunkCount.incrementAndGet();

                            if (!firstChunkSent.get()) {
                                firstChunkSent.set(true);
                                long ttft = System.currentTimeMillis() - startTime;
                                firstChunkTime.set(ttft);

                                log.info("TTFT (time to first chunk): {} ms", ttft);
                            }
                        }
                    })
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
        });
    }

    private Flux<ChatEvent> completeResponse(String conversationId,
                                            ResponseChunkBuffer chunkBuffer,
                                            FollowupMethod method,
                                            List<VectorDatabaseDocument> context,
                                            List<Message> lastMessages){
        Flux<ChatEvent> finalText =
                chunkBuffer.flush()
                .<Flux<ChatEvent>>map(text ->
                        Flux.just(new BotMessageChunk(text)))
                .orElseGet(Flux::empty);

        String answer = chunkBuffer.getFullResponse();
        Message botMessage = MessageFactory.createBotMessage(answer);

        Mono<Void> saveAnswer = Mono.fromRunnable(() -> {
            sendMessage(conversationId, botMessage);
        }).subscribeOn(Schedulers.boundedElastic()).then();



        Flux<ChatEvent> followups = Flux.defer(() ->followupsService.generateFollowups(method, answer, context, lastMessages))
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
    }

    public Flux<ChatEvent> startAndStreamBotResponse(String userContent, FollowupMethod method, Degree level) {
        Conversation conversation = startConversation();
        ChatEvent convId = new ConversationStart(conversation.getId());
        return Flux.concat(Flux.just(convId), streamBotResponse(conversation.getId(), userContent, method, level));
    }
}
