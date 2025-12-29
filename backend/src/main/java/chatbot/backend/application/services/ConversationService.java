package chatbot.backend.application.services;

import chatbot.backend.application.common.interfaces.RerankerGateway;
import chatbot.backend.domain.entities.*;
import chatbot.backend.domain.enums.Sender;
import chatbot.backend.domain.repositories.IConversationRepository;
import chatbot.backend.application.common.interfaces.ChatbotGateway;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private final IConversationRepository conversationRepository;
    private final ChatbotGateway chatbotGateway;
    private final VectorStore vectorStore;
    private final RerankerGateway rerankerGateway;

    @Autowired
    public ConversationService(IConversationRepository conversationRepository,  ChatbotGateway chatbotGateway,  VectorStore vectorStore, RerankerGateway rerankerGateway) {
        this.conversationRepository = conversationRepository;
        this.chatbotGateway = chatbotGateway;
        this.vectorStore = vectorStore;
        this.rerankerGateway = rerankerGateway;
    }

    public Conversation startConversation(){
        Conversation conversation = ConversationFactory.createConversation();
        conversation.setCreatedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public void endConversation(String conversationId){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        conversation.setEndedAt(java.time.Instant.now());
        conversationRepository.save(conversation);
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

    public Flux<String> streamBotResponse(String conversationId, String content) {
        Message userMessage = MessageFactory.createUserMessage(content);
        userMessage.setConversationId(conversationId);
        sendMessage(conversationId, userMessage);

        Conversation conversation = getConversationById(conversationId);

        String rewriteContext = conversation.getMessages().stream()
                .skip(Math.max(0, conversation.getMessages().size() - 4))
                .map(m -> m.getSender() == Sender.BOT
                        ? "BOT: " + m.getContent()
                        : "UŻYTKOWNIK: " + m.getContent())
                .collect(Collectors.joining("\n"));

        String fixedContent = chatbotGateway.rewrite(content, rewriteContext);

        System.out.println(fixedContent);

        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(fixedContent)
                        .topK(15)
                        .similarityThreshold(0.5)
                        .build()
        );

        List<Document> reranked = rerankerGateway.rerank(fixedContent, similarDocs);
        List<Document> limitReranked = reranked.stream().limit(5).toList();

        System.out.println("\n" + limitReranked);

        String faqsContext = limitReranked.isEmpty() ? "Brak dostępnych informacji w bazie wiedzy." : limitReranked.stream()
                .map(d ->
                    """
                    [INFORMACJA]
                    %s
                    """.formatted(d.getText()))
                .collect(Collectors.joining("\n"));

        String shortHistory = conversation.getMessages().stream()
                .skip(Math.max(0, conversation.getMessages().size() - 4))
                .map(m -> m.getSender() == Sender.BOT
                        ? "BOT: " + m.getContent()
                        : "UŻYTKOWNIK: " + m.getContent())
                .collect(Collectors.joining("\n"));

        Prompt prompt = getPrompt(fixedContent, faqsContext, shortHistory);

        StringBuilder responseBuilder = new StringBuilder();
        StringBuilder buffer = new StringBuilder();

        return chatbotGateway.response(prompt)
                .flatMap(chunk -> {
                    buffer.append(chunk);

                    if (buffer.length() >= 120) {
                        String toSend = buffer.toString();
                        buffer.setLength(0);
                        responseBuilder.append(toSend);

                        return Flux.just("data: " + toSend + "\n\n");
                    }

                    return Flux.empty();
                })
                .concatWith(Flux.defer(() -> {
                    Flux<String> finalText = Flux.empty();

                    if (!buffer.isEmpty()) {
                        String remainingData = buffer.toString();
                        buffer.setLength(0);
                        responseBuilder.append(remainingData);

                        finalText = Flux.just("data: " + remainingData + "\n\n");
                    }

                    Message botMessage = MessageFactory.createBotMessage(responseBuilder.toString());
                    botMessage.setConversationId(conversationId);
                    conversation.sendMessage(botMessage);

                    String followupPrompt = "Odpowiedź bota: " + responseBuilder;

                    Flux<String> followups = chatbotGateway.followups(followupPrompt)
                            .map(followupsJson ->
                                    "data: {\"type\":\"followups\",\"options\":" + followupsJson + "}\n\n"
                            )
                            .onErrorReturn("data: {\"type\":\"followups\",\"options\":[]}\n\n");

                    return Flux.concat(finalText, followups);
                }))
                .publishOn(Schedulers.boundedElastic())
                .doFinally(_ -> conversationRepository.save(conversation))
                .onErrorResume(err -> {
                    System.err.println("Streaming error: " + err.getMessage());
                    return Flux.just("ERROR: " + err.getMessage());
                });
    }

    private static @NonNull Prompt getPrompt(String content, String faqsContext, String shortHistory) {
        String systemPrompt =
                """
                Jesteś Tulbotem, chatbotem odpowiadającym wyłącznie na pytania o Politechnice Łódzkiej.
                Odpowiadaj wyłącznie na podstawie przekazanego kontekstu.
                Nie korzystaj z wiedzy spoza kontekstu i niczego nie dopowiadaj.
                Odpowiedzi formułuj krótko i konkretnie.
                Nie cytuj pytania użytkownika.
                Jeśli w kontekście nie ma informacji potrzebnej do odpowiedzi, powiedz o tym wprost.
                Jeśli pytanie nie dotyczy Politechniki Łódzkiej, poinformuj o tym uprzejmie.
                Zawsze pozostawaj w roli Tulbota.
                """;

        String userPrompt =
            """
            [KONTEKST – JEDYNE ŹRÓDŁO WIEDZY]
            %s
            
            [KONTEKST DIALOGOWY – NIE JEST ŹRÓDŁEM WIEDZY]
            %s
            
            [AKTUALNE PYTANIE UŻYTKOWNIKA]
            %s
            """.formatted(faqsContext, shortHistory, content);

        return new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));
    }

    public Flux<String> startAndStreamBotResponse(String userContent) {
        Conversation conversation = startConversation();
        String id = "data: {\"type\":\"conversationId\",\"id\":\"" + conversation.getId() + "\"}\n\n";
        return Flux.concat(Flux.just(id), streamBotResponse(conversation.getId(), userContent));
    }

}
