package chatbot.backend.host.controllers;

import chatbot.backend.application.chat.*;
import chatbot.backend.application.services.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    @Autowired
    public ConversationController(ConversationService conversationService, OllamaChatModel chatModel, ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam String content,
                            @RequestParam(required = false) String conversationId) {

        Flux<ChatEvent> events = (conversationId == null || conversationId.isEmpty()) ? conversationService.startAndStreamBotResponse(content) : conversationService.streamBotResponse(conversationId, content);

        return events.map(event -> switch (event) {
            case BotMessageChunk chunk -> "data: " + chunk.text() + "\n\n";
            case BotFinalMessage finalMessage -> "data: " + finalMessage.finalText() + "\n\n";
            case FollowupsEvent followups ->
                    "data: {\"type\":\"followups\",\"options\":" + toJson(followups.followups()) + "}\n\n";
            case ErrorEvent error ->
                    "data: {\"type\":\"error\",\"message\":\"" + error.text() + "\"}\n\n";
            case ConversationStart convId->
                    "data: {\"type\":\"conversationId\",\"id\":\"" + convId.conversationId() + "\"}\n\n";
        });
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}

