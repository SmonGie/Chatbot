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
    private final OllamaChatModel chatModel;

    @Autowired
    public ConversationController(ConversationService conversationService, OllamaChatModel chatModel) {
        this.conversationService = conversationService;
        this.chatModel = chatModel;
    }

    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam String content,
                            @RequestParam(required = false) String conversationId) {
        Message message = new Message();
        message.setContent(content);

        if (conversationId == null || conversationId.isEmpty()) {
            Conversation conversation = conversationService.startConversation();
            conversationId = conversation.getId();
            message.setConversationId(conversationId);

            String idChunk = "data: {\"conversationId\",\"id\":\"" + conversationId + "\"}\n\n";

            return Flux.concat(
                    Flux.just(idChunk),
                    handleMessage(message)
            );
        } else {
            message.setConversationId(conversationId);
            return handleMessage(message);
        }
    }

    private Flux<String> generateFollowups(String answer) {
        String instruction =
        """
        Na podstawie tej odpowiedzi wygeneruj follow-up questions.
        Zwróć tylko listę pytań w formacie JSON (np. ["pytanie 1", "pytanie 2"]), bez dodatkowego tekstu.

        Odpowiedź:
        %s
        """.formatted(answer);

        return chatModel.stream(new Prompt(instruction))
                .map(chatResponse -> Objects.requireNonNull(chatResponse.getResult().getOutput().getText()))
                .reduce("", (result, chunk) -> result + chunk)
                .map(text -> {
                    text = text.trim();
                    int start = text.indexOf('[');
                    int end = text.lastIndexOf(']') + 1;
                    if (start >= 0 && end > start) {
                        return text.substring(start, end);
                    }
                    return "[]";
                })
                .flux()
                .onErrorReturn("[]");
    }

    private Flux<String> handleMessage(Message message) {
        if (message.getConversationId() == null || message.getConversationId().isEmpty()) {
            Conversation conversation = conversationService.startConversation();
            message.setConversationId(conversation.getId());
        }

        String conversationId = message.getConversationId();
        conversationService.sendMessage(conversationId, message);

        Conversation conversation = conversationService.getConversationById(conversationId);
        String prompt = conversation.getMessages().stream()
                .map(m -> (m.getSender() == BOT ? "Tulbot: " : "Użytkownik: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        StringBuilder Response = new StringBuilder();
        StringBuilder Buffer = new StringBuilder();

        return chatModel.stream(new Prompt(prompt))
                .mapNotNull(chatResponse -> chatResponse.getResult().getOutput().getText())
                .flatMap(chunk -> {
                    Buffer.append(chunk);

                    int sentenceEnd = Math.max(
                            Math.max(Buffer.lastIndexOf("."), Buffer.lastIndexOf("!")),
                            Buffer.lastIndexOf("?")
                    );

                    if (sentenceEnd != -1) {
                        String toSend = Buffer.substring(0, sentenceEnd + 1);
                        Buffer.delete(0, sentenceEnd + 1);
                        Response.append(toSend);
                        return Flux.just("data: " + toSend + "\n\n");
                    }

                    if (Buffer.length() > 100) {
                        String toSend = Buffer.toString();
                        Buffer.setLength(0);
                        Response.append(toSend);
                        return Flux.just("data: " + toSend + "\n\n");
                    }

                    return Flux.empty();
                })
                .concatWith(
                        Flux.defer(() -> {
                            if (!Buffer.isEmpty()) {
                                Response.append(Buffer);
                            }

                            Message botMessage = new Message(BOT, Response.toString());
                            botMessage.setConversationId(conversationId);
                            conversationService.sendMessage(conversationId, botMessage);

                            return generateFollowups(Response.toString())
                                    .next()
                                    .map(followups -> "data: {\"options\":" + followups + "}\n\n")
                                    .onErrorReturn("data: {\"options\":[]}\n\n");
                        })
                )
                .onErrorResume(err -> {
                    System.err.println("Streaming error: " + err.getMessage());
                    return Flux.just("ERROR: " + err.getMessage());
                });
    }
}

