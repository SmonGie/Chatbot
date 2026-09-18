package chatbot.backend.host.controllers;

import chatbot.backend.application.chat.*;
import chatbot.backend.application.enums.Degree;
import chatbot.backend.application.services.ConversationService;
import chatbot.backend.domain.enums.FollowupMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/conversation")
@Tag(name = "Conversation")
public class ConversationController {
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    @Autowired
    public ConversationController(ConversationService conversationService, ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }
    @Operation(
            summary = "Ask the chatbot a question",
            description = """
                Streams chat events using Server-Sent Events.
                Creates a conversation when conversationId is omitted or empty.
                Errors handled within the stream are sent as JSON events
                with type "error".
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SSE stream containing chat events",
                            content = @Content(
                                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = """
                            Missing, blank, or excessively long content,
                            or an invalid method or level value
                            """
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Conversation not found"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = """
                                Unexpected server error before the
                                streaming response is committed
                                """
                    )
            }
    )
    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ObjectNode>> ask(
            @NotBlank(message = "Content must not be blank")
            @Size(max = 2000, message = "Content must not exceed 2000 characters")
            @RequestParam String content,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "RAG") FollowupMethod method,
            @RequestParam(defaultValue = "all") Degree level
    ) {
        Flux<ChatEvent> events =
                (conversationId == null || conversationId.isBlank())
                        ? conversationService.startAndStreamBotResponse(content, method, level)
                        : conversationService.streamBotResponse(conversationId, content, method, level);

        return events.map(event ->
                ServerSentEvent.<ObjectNode>builder()
                        .data(toSseMessage(event))
                        .build()
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

