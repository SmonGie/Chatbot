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
        return conversationRepository.save(conversation);
    }

    public void sendMessage(Message message){
        boolean found = conversationRepository.appendMessage(
                message.getConversationId(),
                message
        );

        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
    }

    public Flux<ChatEvent> streamBotResponse(String conversationId, String query, FollowupMethod method, Degree level) {
        return Flux.defer(() -> {
            long startTime = System.currentTimeMillis();
            AtomicBoolean firstChunkSent = new AtomicBoolean(false);
            AtomicLong firstChunkTime = new AtomicLong(0);
            AtomicInteger chunkCount = new AtomicInteger(0);

            ResponseChunkBuffer chunkBuffer = new ResponseChunkBuffer();
            return Mono.fromCallable(() ->
                            preparePrompt(conversationId, query, level)
                    )
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(prompt ->
                            chatbotGateway.response(prompt)
                                    .flatMap(chunk -> {
                                        List<ChatEvent> events =
                                                chunkBuffer.append(chunk).stream()
                                                        .<ChatEvent>map(BotMessageChunk::new)
                                                        .toList();

                                        return Flux.fromIterable(events);
                                    })
                                    .concatWith(Flux.defer(() ->
                                            completeResponse(
                                                    conversationId,
                                                    chunkBuffer,
                                                    method,
                                                    prompt.knowledgeContext(),
                                                    prompt.history()
                                            )
                                    ))
                    )
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
                    .onErrorResume(error -> {
                        log.warn("Response failed for conversation {}", conversationId, error);
                        String message = "Nie udało się przygotować lub zapisać odpowiedzi.";

                        if (error instanceof ResponseStatusException exception && exception.getStatusCode().value() == 404) {
                            message = "Nie znaleziono rozmowy. Rozpocznij nową rozmowę.";
                        }
                        return Flux.just(new ErrorEvent(message));
                    })
                    .doFinally(_ -> {
                        long totalTime = System.currentTimeMillis() - startTime;

                        log.info("Total latency: {} ms", totalTime);
                        log.info("Chunk count: {}", chunkCount.get());

                        if (firstChunkTime.get() > 0) {
                            long streamingTime = totalTime - firstChunkTime.get();
                            log.info("Streaming duration: {} ms", streamingTime);
                        }
                    });
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
        Message botMessage = MessageFactory.createBotMessage(conversationId, answer);

        Mono<Void> saveAnswer = Mono.fromRunnable(() -> sendMessage(botMessage)).subscribeOn(Schedulers.boundedElastic()).then();

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
        return Mono.fromCallable(this::startConversation)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(conversation ->
                        Flux.concat(
                                Flux.<ChatEvent>just(new ConversationStart(conversation.getId())),
                                streamBotResponse(conversation.getId(), userContent, method, level)
                        )
                )
                .onErrorResume(error -> {
                    log.warn("New conversation failed", error);

                    return Flux.just(new ErrorEvent("Nie udało się utworzyć nowej rozmowy."));
                });
    }

    private PromptMessage preparePrompt(String conversationId, String query, Degree level){
        Message userMessage = MessageFactory.createUserMessage(conversationId, query);
        sendMessage(userMessage);

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        List<Message> history = new ArrayList<>(conversation.getLastMessages(4));

        String rewrittenQuery = chatbotGateway.rewrite(query, history);

        List<VectorDatabaseDocument> documents =vectorDatabaseSearchGateway.findSimilarDocuments(rewrittenQuery, 30, level);
        List<VectorDatabaseDocument> context = rerankerGateway.rerank(rewrittenQuery, documents).stream().limit(5).toList();

        return new PromptMessage(rewrittenQuery, history, context);
    }
}
