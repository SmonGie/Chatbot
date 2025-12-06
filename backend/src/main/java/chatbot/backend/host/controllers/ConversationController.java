package chatbot.backend.host.controllers;

import chatbot.backend.application.services.ConversationService;
import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.entities.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


import java.util.stream.Collectors;

import static chatbot.backend.domain.enums.Sender.BOT;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    private final ConversationService conversationService;
    private final OllamaChatModel chatModel;

    @Autowired
    public ConversationController(ConversationService conversationService, OllamaChatModel chatModel) {
        this.conversationService = conversationService;
        this.chatModel = chatModel;
    }


    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestBody Message message) {
        if (message.getConversationId() == null || message.getConversationId().isEmpty()) {
            Conversation conversation = conversationService.startConversation();
            message.setConversationId(conversation.getId());
        }

        conversationService.sendMessage(message.getConversationId(), message);

        Conversation conversation = conversationService.getConversationById(message.getConversationId());
        String prompt = conversation.getMessages().stream()
                .map(m -> (m.getSender() == BOT ? "Bot: " : "User: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        StringBuilder fullResponse = new StringBuilder();
        String conversationId = message.getConversationId();

        return chatModel.stream(new Prompt(prompt))
                .mapNotNull(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getText();
                    fullResponse.append(content);
                    return content;
                })
                .doOnComplete(() -> {
                    Message botMessage = new Message(BOT, fullResponse.toString());
                    botMessage.setConversationId(conversationId);
                    conversationService.sendMessage(conversationId, botMessage);
                })
                .onErrorResume(e -> Flux.just("data: {\"error\": \"" + e.getMessage() + "\"}\n\n"));
    }

}

