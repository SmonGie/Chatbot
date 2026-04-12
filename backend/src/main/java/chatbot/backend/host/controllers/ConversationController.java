package chatbot.backend.host.controllers;

import chatbot.backend.application.chat.*;
import chatbot.backend.application.enums.Degree;
import chatbot.backend.application.services.ConversationService;
import chatbot.backend.domain.enums.FollowupMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
    public Flux<ServerSentEvent<String>> ask(
            @RequestParam String content,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false, defaultValue = "RAG") FollowupMethod method,
            @RequestParam(required = false, defaultValue = "all") Degree level
    ) {
        Flux<ChatEvent> events =
                (conversationId == null || conversationId.isEmpty())
                        ? conversationService.startAndStreamBotResponse(content, method, level)
                        : conversationService.streamBotResponse(conversationId, content, method, level);

        return events.map(event ->
                {
                    try {
                        return ServerSentEvent.<String>builder()
                                .data(objectMapper.writeValueAsString(toSseMessage(event)))
                                .build();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private ObjectNode toSseMessage(ChatEvent event) {
        return switch (event) {
            case BotMessageChunk chunk -> objectMapper.createObjectNode()
                    .put("type", "chunk")
                    .put("text", chunk.text());

            case BotFinalMessage finalMessage -> objectMapper.createObjectNode()
                    .put("type", "final")
                    .put("text", finalMessage.finalText());

            case FollowupsEvent followups -> objectMapper.createObjectNode()
                    .put("type", "followups")
                    .set("options", objectMapper.valueToTree(followups.followups()));

            case ErrorEvent error -> objectMapper.createObjectNode()
                    .put("type", "error")
                    .put("message", error.text());

            case ConversationStart convId -> objectMapper.createObjectNode()
                    .put("type", "conversationId")
                    .put("id", convId.conversationId());
        };
    }
}

