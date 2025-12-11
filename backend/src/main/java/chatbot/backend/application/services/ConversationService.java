package chatbot.backend.application.services;

import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.entities.Message;
import chatbot.backend.domain.enums.Sender;
import chatbot.backend.domain.repositories.IConversationRepository;
import chatbot.backend.infrastructure.adapters.ChatbotGateway;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
public class ConversationService {
    private final IConversationRepository conversationRepository;
    private final ChatbotGateway chatbotGateway;

    @Autowired
    public ConversationService(IConversationRepository conversationRepository,  ChatbotGateway chatbotGateway) {
        this.conversationRepository = conversationRepository;
        this.chatbotGateway = chatbotGateway;
    }

    public Conversation startConversation(){
        Conversation conversation = new Conversation();
        conversation.setCreatedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation endConversation(String conversationId){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        conversation.setEndedAt(java.time.Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation sendMessage(String conversationId, Message message){
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        message.setConversationId(conversationId);
        conversation.sendMessage(message);
        return conversationRepository.save(conversation);
    }

    public Conversation getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }

    public Flux<String> streamBotResponse(String conversationId, String userContent) {
        Message userMessage = new Message(Sender.USER, userContent);
        userMessage.setConversationId(conversationId);
        sendMessage(conversationId, userMessage);

        Conversation conversation = getConversationById(conversationId);

        String systemInstruction =
        """
        Jesteś Tulbotem, chatbotem odpowiadającym na pytania o Politechnice Łódzkiej.
        Odpowiadaj wyłącznie krótkimi, konkretnymi odpowiedziami.
        Nie dodawaj wyjaśnień, komentarzy ani dodatkowego tekstu.
        """;

        String promptText = systemInstruction + "\n\n" + conversation.getMessages().stream()
                .map(m -> (m.getSender() == Sender.BOT ? "Tulbot: " : "Użytkownik: ") + m.getContent())
                .collect(Collectors.joining("\n"));
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
                    Message botMessage = new Message(Sender.BOT, responseBuilder.toString());
                    botMessage.setConversationId(conversationId);
                    sendMessage(conversationId, botMessage);

                    return chatbotGateway.followups(responseBuilder.toString())
                            .map(followups -> "data: {\"type\":\"followups\",\"options\":" + followups + "}\n\n")
                            .onErrorReturn("data: {\"options\":[]}\n\n");
                }))
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
