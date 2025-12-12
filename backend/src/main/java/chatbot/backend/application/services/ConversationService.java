package chatbot.backend.application.services;

import chatbot.backend.domain.entities.*;
import chatbot.backend.domain.enums.Sender;
import chatbot.backend.domain.repositories.IConversationRepository;
import chatbot.backend.infrastructure.adapters.ChatbotGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    public ConversationService(IConversationRepository conversationRepository,  ChatbotGateway chatbotGateway,  VectorStore vectorStore) {
        this.conversationRepository = conversationRepository;
        this.chatbotGateway = chatbotGateway;
        this.vectorStore = vectorStore;
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

        String systemInstruction =
        """
        Jesteś Tulbotem, chatbotem odpowiadającym na pytania o Politechnice Łódzkiej.
        Odpowiadaj wyłącznie krótkimi, konkretnymi odpowiedziami.
        Nie dodawaj wyjaśnień, komentarzy ani dodatkowego tekstu.
        Nie powtarzaj pytań użytkownika ani historii czatu.
        Jeśli pytanie nie dotyczy Politechniki Łódzkiej, grzecznie o tym poinformuj i zaproponuj
        zadanie pytania związanego z Politechniką Łódzką.
        """;

        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(content)
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build()
        );

        String faqsContext = similarDocs.stream()
                .map(d -> "FAQ: " + d.getMetadata().get("question") + "\n" + d.getText())
                .collect(Collectors.joining("\n\n"));

//        String promptText = systemInstruction + "\n\n" + faqsContext +"\n\n" + conversation.getMessages().stream()
//                .map(m -> (m.getSender() == Sender.BOT ? "Tulbot: " : "Użytkownik: ") + m.getContent())
//                .collect(Collectors.joining("\n"));
        String promptText = systemInstruction + "\n\n" + faqsContext + "\n\n"
                + "Pytanie użytkownika: " + content + "\nTulbot:";
        Prompt prompt = new Prompt(promptText);

        StringBuilder responseBuilder = new StringBuilder();
        StringBuilder buffer = new StringBuilder();

        return chatbotGateway.response(prompt)
                .flatMap(chunk -> {
                    buffer.append(chunk);
                    int sentenceEnd = Math.max(Math.max(buffer.lastIndexOf("."), buffer.lastIndexOf("!")), buffer.lastIndexOf("?"));
                    if (sentenceEnd != -1) {
                        String toSend = buffer.substring(0, sentenceEnd + 1);
                        buffer.delete(0, sentenceEnd + 1);
                        responseBuilder.append(toSend);
                        return Flux.just("data: " + toSend + "\n\n");
                    }
                    if (buffer.length() > 100) {
                        String toSend = buffer.toString();
                        buffer.setLength(0);
                        responseBuilder.append(toSend);
                        return Flux.just("data: " + toSend + "\n\n");
                    }
                    return Flux.empty();
                })
                .concatWith(Flux.defer(() -> {
                    if (!buffer.isEmpty()) {
                        responseBuilder.append(buffer);
                    }
                    Message botMessage = MessageFactory.createBotMessage(responseBuilder.toString());
                    botMessage.setConversationId(conversationId);
                    conversation.sendMessage(botMessage);

                    String followupPrompt = "Na podstawie poniższych FAQ i ostatniej odpowiedzi bota, zaproponuj 3 możliwe kolejne pytania użytkownika:\n\n"
                            + faqsContext + "\n\nOdpowiedź bota: " + responseBuilder.toString();

                    return chatbotGateway.followups(followupPrompt)
                            .map(followupsJson -> {
                                ObjectMapper mapper = new ObjectMapper();
                                List<String> followupStrings;
                                try {
                                    followupStrings = mapper.readValue(followupsJson, new TypeReference<List<String>>() {});
                                } catch (Exception e) {
                                    followupStrings = List.of();
                                }

                                List<FollowUpQuestion> followUpObjects = followupStrings.stream()
                                        .map(f -> FollowUpQuestionFactory.create(botMessage.getId(), f))
                                        .toList();

                                botMessage.setFollowUpQuestions(followUpObjects);

                                return "data: {\"type\":\"followups\",\"options\":" + followupsJson + "}\n\n";
                            })
                            .onErrorReturn("data: {\"options\":[]}\n\n");
                }))
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signalType -> {
                    conversationRepository.save(conversation);
                })
                .onErrorResume(err -> {
                    System.err.println("Streaming error: " + err.getMessage());
                    return Flux.just("ERROR: " + err.getMessage());
                });
    }

    public Flux<String> startAndStreamBotResponse(String userContent) {
        Conversation conversation = startConversation();
        String idChunk = "data: {\"type\":\"conversationId\",\"id\":\"" + conversation.getId() + "\"}\n\n";
        return Flux.concat(Flux.just(idChunk), streamBotResponse(conversation.getId(), userContent));
    }

}
