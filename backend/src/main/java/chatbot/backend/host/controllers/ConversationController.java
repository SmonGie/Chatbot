package chatbot.backend.host.controllers;

import chatbot.backend.application.services.ConversationService;
import chatbot.backend.domain.entities.Conversation;
import chatbot.backend.domain.entities.Message;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static chatbot.backend.domain.enums.Sender.BOT;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    private final ConversationService conversationService;

    @Autowired
    public ConversationController(ConversationService conversationService, OllamaChatModel chatModel) {
        this.conversationService = conversationService;
    }

    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam String content,
                            @RequestParam(required = false) String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return conversationService.startAndStreamBotResponse(content);
        } else {
            return conversationService.streamBotResponse(conversationId, content);
        }
    }
}

